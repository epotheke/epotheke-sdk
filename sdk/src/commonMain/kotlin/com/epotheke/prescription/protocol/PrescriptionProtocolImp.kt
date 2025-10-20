/****************************************************************************
 * Copyright (C) 2024 ecsec GmbH.
 * All rights reserved.
 * Contact: ecsec GmbH (info@ecsec.de)
 *
 * This file is part of the epotheke SDK.
 *
 * GNU General Public License Usage
 * This file may be used under the terms of the GNU General Public
 * License version 3.0 as published by the Free Software Foundation
 * and appearing in the file LICENSE.GPL included in the packaging of
 * this file. Please review the following information to ensure the
 * GNU General Public License version 3.0 requirements will be met:
 * http://www.gnu.org/copyleft/gpl.html.
 *
 * Other Usage
 * Alternatively, this file may be used in accordance with the terms
 * and conditions contained in a signed written agreement between
 * you and ecsec GmbH.
 *
 ***************************************************************************/

package com.epotheke.prescription.protocol

import com.epotheke.CardLinkProtocol
import com.epotheke.CardLinkProtocolBase
import com.epotheke.Websocket
import com.epotheke.prescription.model.AvailablePrescriptionLists
import com.epotheke.prescription.model.GenericErrorMessage
import com.epotheke.prescription.model.GenericErrorResultType
import com.epotheke.prescription.model.PrescriptionMessage
import com.epotheke.prescription.model.RequestPrescriptionList
import com.epotheke.prescription.model.SelectedPrescriptionList
import com.epotheke.prescription.model.SelectedPrescriptionListResponse
import com.epotheke.prescription.model.prescriptionJsonFormatter
import com.epotheke.protocolChannel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val logger = KotlinLogging.logger {}
private const val RECEIVE_TIMEOUT_SECONDS = 30L

class PrescriptionProtocolException(
    val msg: GenericErrorMessage,
) : Exception()

interface PrescriptionProtocol : CardLinkProtocol {
    @Throws(PrescriptionProtocolException::class, CancellationException::class)
    suspend fun requestPrescriptions(req: RequestPrescriptionList): AvailablePrescriptionLists

    @OptIn(ExperimentalUuidApi::class)
    @Throws(PrescriptionProtocolException::class, CancellationException::class)
    suspend fun requestPrescriptions(
        iccsns: List<String> = emptyList(),
        messageId: String = Uuid.random().toString(),
    ): AvailablePrescriptionLists

    @Throws(PrescriptionProtocolException::class, CancellationException::class)
    suspend fun selectPrescriptions(selection: SelectedPrescriptionList): SelectedPrescriptionListResponse
}

class PrescriptionProtocolImp(
    private val ws: Websocket,
) : CardLinkProtocolBase(ws),
    PrescriptionProtocol {
    private val inputChannel = protocolChannel<PrescriptionMessage>()

    override fun messageHandler(msg: String): (suspend () -> Unit)? {
        try {
            val envelope = prescriptionJsonFormatter.decodeFromString<PrescriptionMessage>(msg)
            return { inputChannel.send(envelope) }
        } catch (_: Exception) {
            return null
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun requestPrescriptions(
        iccsns: List<String>,
        messageId: String,
    ): AvailablePrescriptionLists =
        requestPrescriptions(
            RequestPrescriptionList(
                iccsns.map { s -> s.hexToByteArray() },
                messageId,
            ),
        )

    private fun checkCorrelation(
        messageId: String,
        correlationId: String,
    ) {
        if (messageId != correlationId) {
            throw PrescriptionProtocolException(
                GenericErrorMessage(
                    errorCode = GenericErrorResultType.INVALID_MESSAGE_DATA,
                    errorMessage = "The received message was not valid.",
                    correlationId = messageId,
                ),
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend inline fun <reified T : PrescriptionMessage> receive(reqMessageId: String): T {
        val lastTimestampUntilTimeout = Clock.System.now().epochSeconds + RECEIVE_TIMEOUT_SECONDS
        var duration = RECEIVE_TIMEOUT_SECONDS.seconds
        while (true) {
            try {
                val response =
                    withTimeout(
                        timeout = duration,
                    ) {
                        inputChannel.receive()
                    }
                when (
                    response
                ) {
                    is T -> return response
                    is GenericErrorMessage -> {
                        logger.debug { "Received generic error: ${response.errorMessage}" }
                        throw PrescriptionProtocolException(response)
                    }

                    else -> {
                        duration = (lastTimestampUntilTimeout - Clock.System.now().epochSeconds).seconds
                    }
                }
                // this might happen on reconnect since server sends session information as a hello
            } catch (e: SerializationException) {
                logger.warn { "Invalid message type received - Ignoring" }
            } catch (e: TimeoutCancellationException) {
                logger.error { "Timeout during receive" }
                throw PrescriptionProtocolException(
                    GenericErrorMessage(
                        errorCode = GenericErrorResultType.UNKNOWN_ERROR,
                        errorMessage = "Timeout",
                        correlationId = reqMessageId,
                    ),
                )
            }
        }
    }

    override suspend fun requestPrescriptions(req: RequestPrescriptionList): AvailablePrescriptionLists {
        logger.debug { "Sending data to request eReceipts." }
        try {
            ws.send(prescriptionJsonFormatter.encodeToString(req))
            val resp = receive<AvailablePrescriptionLists>(reqMessageId = req.messageId)
            checkCorrelation(req.messageId, resp.correlationId)
            return resp
        } catch (e: Exception) {
            when (e) {
                is PrescriptionProtocolException -> throw e
                else -> {
                    logger.error(e) { "Unspecified error" }
                    throw PrescriptionProtocolException(
                        GenericErrorMessage(
                            errorCode = GenericErrorResultType.UNKNOWN_ERROR,
                            errorMessage = "Unspecified error",
                            correlationId = req.messageId,
                        ),
                    )
                }
            }
        }
    }

    override suspend fun selectPrescriptions(selection: SelectedPrescriptionList): SelectedPrescriptionListResponse {
        logger.debug { "Sending data to select prescriptions." }
        try {
            ws.send(prescriptionJsonFormatter.encodeToString(selection))
            val resp = receive<SelectedPrescriptionListResponse>(reqMessageId = selection.messageId)
            checkCorrelation(selection.messageId, resp.correlationId)
            return resp
        } catch (e: Exception) {
            when (e) {
                is PrescriptionProtocolException -> throw e
                else -> {
                    logger.error(e) { "Unspecified error" }
                    throw PrescriptionProtocolException(
                        GenericErrorMessage(
                            errorCode = GenericErrorResultType.UNKNOWN_ERROR,
                            errorMessage = "Unspecified error",
                            correlationId = selection.messageId,
                        ),
                    )
                }
            }
        }
    }
}
