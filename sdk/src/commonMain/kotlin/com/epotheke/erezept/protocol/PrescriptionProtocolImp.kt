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

package com.epotheke.erezept.protocol

import com.epotheke.erezept.model.AvailablePrescriptionLists
import com.epotheke.erezept.model.GenericErrorMessage
import com.epotheke.erezept.model.GenericErrorResultType
import com.epotheke.erezept.model.PrescriptionMessage
import com.epotheke.erezept.model.RequestPrescriptionList
import com.epotheke.erezept.model.SelectedPrescriptionList
import com.epotheke.erezept.model.SelectedPrescriptionListResponse
import com.epotheke.erezept.model.prescriptionJsonFormatter
import com.epotheke.sdk.CardLinkProtocol
import com.epotheke.sdk.CardLinkProtocolBase
import com.epotheke.sdk.WebsocketCommon
import com.epotheke.sdk.now
import com.epotheke.sdk.protocolChannel
import com.epotheke.sdk.randomUUID
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}
private const val RECEIVE_TIMEOUT_SECONDS = 30L

class PrescriptionProtocolException(
    val msg: GenericErrorMessage,
) : Exception()

interface PrescriptionProtocol : CardLinkProtocol {
    @Throws(PrescriptionProtocolException::class, CancellationException::class)
    suspend fun requestPrescriptions(req: RequestPrescriptionList): AvailablePrescriptionLists

    @Throws(PrescriptionProtocolException::class, CancellationException::class)
    suspend fun requestPrescriptions(
        iccsns: List<String> = emptyList(),
        messageId: String = randomUUID(),
    ): AvailablePrescriptionLists

    @Throws(PrescriptionProtocolException::class, CancellationException::class)
    suspend fun selectPrescriptions(selection: SelectedPrescriptionList): SelectedPrescriptionListResponse
}

class PrescriptionProtocolImp(
    private val ws: WebsocketCommon,
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

    private suspend inline fun <reified T : PrescriptionMessage> receive(reqMessageId: String): T {
        val lastTimestampUntilTimeout = now() + RECEIVE_TIMEOUT_SECONDS
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
                        duration = (lastTimestampUntilTimeout - now()).seconds
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
