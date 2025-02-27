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

package com.epotheke.sdk

import com.epotheke.erezept.model.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}
private const val ReceiveTimeoutSeconds = 30L

class PrescriptionProtocolImp(
    private val ws: WebsocketCommon,
) : CardLinkProtocolBase(), PrescriptionProtocol {

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun requestPrescriptions(iccsns: List<String>, messageId: String): AvailablePrescriptionLists {
        return requestPrescriptions(
            RequestPrescriptionList(
                iccsns.map { s -> s.hexToByteArray() },
                messageId
            )
        )
    }

    private fun checkCorrelation(messageId: String, correlationId: String) {
        if (messageId != correlationId) {
            throw PrescriptionProtocolException(
                GenericErrorMessage(
                    errorCode = GenericErrorResultType.INVALID_MESSAGE_DATA,
                    errorMessage = "The received message was not valid.",
                    correlationId = messageId
                )
            )
        }
    }

    private suspend inline fun <reified T : PrescriptionMessage> receive(reqMessageId: String): T {
        val lastTimestampUntilTimeout = now() + ReceiveTimeoutSeconds
        var duration = ReceiveTimeoutSeconds.seconds
        while (true) {
            try {
                val response = withTimeout(
                    timeout = duration
                ) {
                    inputChannel.receive()
                }
                when (val prescriptionMessage =
                    prescriptionJsonFormatter.decodeFromString<PrescriptionMessage>(response)) {
                    is T -> return prescriptionMessage
                    is GenericErrorMessage -> {
                        logger.debug { "Received generic error: ${prescriptionMessage.errorMessage}" }
                        throw PrescriptionProtocolException(prescriptionMessage)
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
                        correlationId = reqMessageId
                    )
                )

            }
        }
    }

    override suspend fun requestPrescriptions(req: RequestPrescriptionList): AvailablePrescriptionLists {
        logger.debug { "Sending data to request eReceipts." }
        try {
            if(!ws.isOpen()){
                logger.debug { "Cannot send since ws is closed - reconnecting." }
                ws.connect()
            }
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
                            correlationId = req.messageId
                        )
                    )
                }
            }
        }
    }

    override suspend fun selectPrescriptions(selection: SelectedPrescriptionList): SelectedPrescriptionListResponse {
        logger.debug { "Sending data to select prescriptions." }
        try {
            if(!ws.isOpen()){
                logger.debug { "Cannot send since ws is closed - reconnecting." }
                ws.connect()
            }
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
                            correlationId = selection.messageId
                        )
                    )
                }
            }
        }
    }

}

