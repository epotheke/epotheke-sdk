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

import WebsocketCommon
import com.epotheke.erezept.model.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import now
import kotlin.time.Duration.Companion.seconds

//import java.time.LocalDateTime.now
//import java.util.concurrent.TimeoutException
//import kotlin.time.Duration.Companion.seconds
//import kotlin.time.toKotlinDuration

private val logger = KotlinLogging.logger {}
private const val ReceiveTimeoutSeconds = 30L

class PrescriptionProtocolImp(
    private val ws: WebsocketCommon,
) : CardLinkProtocolBase(), PrescriptionProtocol {

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun requestPrescriptions(req: RequestPrescriptionList): AvailablePrescriptionLists {
        logger.debug { "Sending data to request ecreipts." }

        ws.send(
            prescriptionJsonFormatter.encodeToString(req)
        )

        val lastTimestampUntilTimeout = now() + ReceiveTimeoutSeconds
        var duration = ReceiveTimeoutSeconds.seconds
        while (true) {
            try {
                val response = withTimeout(
                    timeout = duration
                ) {
                    inputChannel.receive()
                }
                when (val prescriptionMessage = prescriptionJsonFormatter.decodeFromString<PrescriptionMessage>(response)) {
                    is AvailablePrescriptionLists -> {
                        if (prescriptionMessage.correlationId == req.messageId) {
                            return prescriptionMessage
                        }
                    }

                    is GenericErrorMessage -> {
                        throw PrescriptionProtocolException(prescriptionMessage)
                    }

                    else -> {
                        duration = (lastTimestampUntilTimeout - now()).seconds
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is TimeoutCancellationException -> {
                        logger.error { "Timeout during receive" }
                        throw PrescriptionProtocolException(
                            GenericErrorMessage(
                                errorCode = GenericErrorResultType.UNKNOWN_ERROR,
                                errorMessage = "Timeout",
                                correlationId = req.messageId
                            )
                        )
                    }
                }
                logger.error(e) { "Exception during receive" }
                throw e
            }
        }
    }

    override suspend fun selectPrescriptions(lst: SelectedPrescriptionList): SelectedPrescriptionListResponse {
        logger.debug { "Sending data to select prescriptions." }
        ws.send(
            prescriptionJsonFormatter.encodeToString(lst)
        )


        val lastTimestampUntilTimeout = now() + ReceiveTimeoutSeconds
        var duration = ReceiveTimeoutSeconds.seconds

        while (true) {
            try {
                val response = withTimeout(
                     timeout = duration
                ) {
                    inputChannel.receive()
                }
                when (val prescriptionMessage = prescriptionJsonFormatter.decodeFromString<PrescriptionMessage>(response)) {
                    is SelectedPrescriptionListResponse -> {
                        if (prescriptionMessage.correlationId == lst.messageId) {
                            return prescriptionMessage
                        }
                    }

                    is GenericErrorMessage -> {
                        throw PrescriptionProtocolException(prescriptionMessage)
                    }

                    else -> {
                        duration = (lastTimestampUntilTimeout - now()).seconds
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is TimeoutCancellationException -> {
                        logger.error { "Timeout during receive" }
                        throw PrescriptionProtocolException(
                            GenericErrorMessage(
                                errorCode = GenericErrorResultType.UNKNOWN_ERROR,
                                errorMessage = "Timeout",
                                correlationId = lst.messageId
                            )
                        )
                    }
                }
                logger.error(e) { "Exception during receive" }
                throw e
            }
        }
    }

}

