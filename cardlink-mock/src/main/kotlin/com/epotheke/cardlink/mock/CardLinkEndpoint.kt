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

package com.epotheke.cardlink.mock

import com.epotheke.cardlink.mock.encoding.GematikMessageEncoder
import com.epotheke.cardlink.mock.encoding.PrescriptionMessageEncoder
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.handler.codec.http.QueryStringDecoder
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.websocket.*
import jakarta.websocket.CloseReason.CloseCodes
import jakarta.websocket.server.ServerEndpoint
import java.util.UUID
import kotlin.random.Random


private val logger = KotlinLogging.logger {}

@ApplicationScoped
@ServerEndpoint("/cardlink", subprotocols = ["cardlink"], encoders = [ GematikMessageEncoder::class, PrescriptionMessageEncoder::class])
class CardLinkEndpoint {

    @Inject
    lateinit var objMapper: ObjectMapper

    @Inject
    lateinit var smsSender: SpryngsmsSender

    @Inject
    lateinit var smsCodeHandler: SMSCodeHandler

    companion object {
        const val mseCorrelationId = "mseMessage"
        const val internalAuthCorrelationId = "internalAuthMessage"
        val registerEgkCorrelationIds = mutableMapOf<String, String>()
    }

    @OnOpen
    fun onOpen(session: Session, cfg: EndpointConfig) {
        val webSocketId = getWebSocketId(session)

        if (webSocketId == null) {
            session.close(CloseReason(CloseCodes.PROTOCOL_ERROR, "No webSocketID was provided."))
        } else {
            logger.debug { "New WebSocket connection with ID: $webSocketId." }
            handleWSConnect(session, webSocketId)
        }
    }

    @OnClose 
    fun onClose(session:Session, reason: CloseReason) {
        val webSocketId = getWebSocketId(session)
        logger.debug { "WebSocket connection with ID: $webSocketId was closed. Reason: $reason" }
    }

    @OnError
    fun onError(session: Session, t: Throwable) {
        val webSocketId = getWebSocketId(session)
        logger.debug(t) { "An error occurred for WebSocket connection with ID: $webSocketId." }
    }

    @OnMessage
    fun onMessage(session: Session, data: String) {
        logger.debug { "Received Gematik message in CardLink-Mock: $data" }

        try {
            val gematikMessage = cardLinkJsonFormatter.decodeFromString<GematikEnvelope>(data)

            when (gematikMessage.payload) {
                is SendPhoneNumber -> handleRequestSmsCode(gematikMessage, session)
                is SendTan -> handleConfirmSmsCode(gematikMessage, session)
                is RegisterEgk -> handleRegisterEgkPayload(gematikMessage, session)
                is SendApduResponse -> handleApduResponse(gematikMessage, session)
                is TasklistErrorPayload -> logger.debug { "Received Tasklist Error Envelope message." }
                else -> logger.error { "Unsupported Gematik message: ${gematikMessage::class.java}" }
            }
        } catch (ex: IllegalArgumentException) {
            when (val prescriptionMessage = prescriptionJsonFormatter.decodeFromString<PrescriptionMessage>(data)) {
                is RequestPrescriptionList -> sendPrescriptionList(prescriptionMessage, session)
                is SelectedPrescriptionList -> sendConfirmSelectedPrescriptionList(prescriptionMessage, session)
                else -> throw IllegalStateException("Received not supported message.")
            }
        }
    }

    private fun sendPrescriptionList(prescriptionMessage: RequestPrescriptionList, session: Session) {

        val availablePrescriptionLists = getAvailablePrescriptionListsExample(
            UUID.randomUUID().toString(),
            prescriptionMessage.messageId
        )

        session.asyncRemote.sendObject(availablePrescriptionLists) {
            if (it.exception != null) {
                logger.debug(it.exception) { "Unable to send message." }
            }
        }
    }

    private fun sendConfirmSelectedPrescriptionList(prescriptionMessage: SelectedPrescriptionList, session: Session) {
        val confirmSelectedPrescriptionList = SelectedPrescriptionListResponse(
            messageId = UUID.randomUUID().toString(),
            correlationId = prescriptionMessage.messageId
        )

        session.asyncRemote.sendObject(confirmSelectedPrescriptionList) {
            if (it.exception != null) {
                logger.debug(it.exception) { "Unable to send message." }
            }
        }
    }

    private fun handleWSConnect(session: Session, webSocketId: String) {
        val cardSessionId = UUID.randomUUID().toString()

        val gematikEnvelope = GematikEnvelope(
            SessionInformation(webSocketId, false),
            null,
            cardSessionId,
        )

        session.asyncRemote.sendObject(gematikEnvelope) {
            if (it.exception != null) {
                logger.debug(it.exception) { "Unable to send message." }
            }
        }
    }

    private fun handleConfirmSmsCode(sendTanMessage: GematikEnvelope, session: Session) {
        val cardSessionId = sendTanMessage.cardSessionId
        val correlationId = sendTanMessage.correlationId
        val sendTan = sendTanMessage.payload

        if (cardSessionId == null || correlationId == null) {
            val errorMsg = "Didn't receive a cardSessionId or correlationId."
            logger.error { errorMsg }
            throw IllegalStateException(errorMsg)
        }

        if (sendTan == null) {
            val errorMsg = "Payload is null."
            logger.error { errorMsg }
            sendError(session, errorMsg, cardSessionId, correlationId, 400)
            return
        }

        if (sendTan !is SendTan) {
            val errorMsg = "Payload is not from type: ConfirmTan."
            logger.error { errorMsg }
            sendError(session, errorMsg, cardSessionId, correlationId, 400)
            return
        }

        logger.debug { "Received 'confirmSmsCode' with sms code: '${sendTan.tan}'." }

        val webSocketId = getWebSocketId(session)
        var errorMsg : String? = null
        var minor : MinorResultCode? = null

        if (webSocketId != null) {
            try {
                val correctSMSCode = smsCodeHandler.checkSMSCode(webSocketId, sendTan.tan)
                if (!correctSMSCode) {
                    val error = "Tan is incorrect."
                    logger.error { error }
                    minor = MinorResultCode.TAN_INCORRECT
                    errorMsg = error
                }
            } catch (ex: MaxTriesReached) {
                val error = "Reached max tries for SMS-Code confirmation."
                logger.error { error }
                minor = MinorResultCode.TAN_RETRY_LIMIT_EXCEEDED
                errorMsg = error
            }
        }

        logger.debug { "Sending out confirmation for TAN ..." }

        val confirmTan = ConfirmTan(minor, errorMsg)
        val confirmTanMessage = GematikEnvelope(confirmTan, correlationId, cardSessionId)

        session.asyncRemote.sendObject(confirmTanMessage) {
            if (it.exception != null) {
                logger.debug(it.exception) { "Unable to send message." }
            }
        }
    }

    private fun handleRequestSmsCode(requestSmsTanMessage: GematikEnvelope, session: Session) {
        val cardSessionId = requestSmsTanMessage.cardSessionId
        val correlationId = requestSmsTanMessage.correlationId
        val requestSmsTan = requestSmsTanMessage.payload

        if (cardSessionId == null || correlationId == null) {
            val errorMsg = "Didn't receive a cardSessionId or correlationId."
            logger.error { errorMsg }
            throw IllegalStateException(errorMsg)
        }

        if (requestSmsTan == null) {
            val errorMsg = "Payload is null."
            logger.error { errorMsg }
            sendError(session, errorMsg, cardSessionId, correlationId, 400)
            return
        }

        if (requestSmsTan !is SendPhoneNumber) {
            val errorMsg = "Payload is not from type: SendPhoneNumber."
            logger.error { errorMsg }
            sendError(session, errorMsg, cardSessionId, correlationId, 400)
            return
        }

        val originalPhoneNumber = requestSmsTan.phoneNumber
        val isGermanNumber = smsSender.isGermanPhoneNumber(originalPhoneNumber)

        if (! isGermanNumber) {
            val errorMsg = "Not a German phone number."
            logger.error { errorMsg }
            sendError(session, errorMsg, cardSessionId, correlationId, 400)
            return
        }

        val phoneNumber = smsSender.phoneNumberToInternationalFormat(originalPhoneNumber, "DE")

        logger.debug { "Received 'requestSmsCode' with phone number: '$originalPhoneNumber'." }
        logger.debug { "Got phone number: '$originalPhoneNumber' / International Form: $phoneNumber" }
        logger.debug { "Sending SMS out to '$phoneNumber'." }

        val webSocketId = getWebSocketId(session)

        val confirmPhoneNumber = if (webSocketId != null) {
            val smsCode = smsCodeHandler.createSMSCode(webSocketId)
            val smsCreateMessage = SMSCreateMessage(
                recipient = phoneNumber,
                smsCode = smsCode
            )
            smsSender.createMessage(smsCreateMessage)

            ConfirmPhoneNumber(null, null)
        } else {
            val error = "Unable to get WebSocketID from query parameters."
            logger.error { error }
            ConfirmPhoneNumber(MinorResultCode.UNKNOWN_ERROR, error)
        }

        val gematikMessage = GematikEnvelope(confirmPhoneNumber, correlationId, cardSessionId)
        session.asyncRemote.sendObject(gematikMessage) {
            if (it.exception != null) {
                logger.debug(it.exception) { "Unable to send message." }
            }
        }
    }

    fun handleRegisterEgkPayload(registerEgkMessage: GematikEnvelope, session: Session) {
        val cardSessionId = registerEgkMessage.cardSessionId
        val correlationId = registerEgkMessage.correlationId
        val registerEgk = registerEgkMessage.payload

        if (cardSessionId == null || correlationId == null) {
            val errorMsg = "Didn't receive a cardSessionId or correlationId."
            logger.error { errorMsg }
            throw IllegalStateException(errorMsg)
        }

        if (registerEgk == null) {
            val errorMsg = "Payload is null."
            logger.error { errorMsg }
            sendError(session, errorMsg, cardSessionId, correlationId, 400)
            return
        }

        if (registerEgk !is RegisterEgk) {
            val errorMsg = "Payload is not from Type: RegisterEgk."
            logger.error { errorMsg }
            sendError(session, errorMsg, cardSessionId, correlationId, 400)
            return
        }

        registerEgkCorrelationIds[cardSessionId] = correlationId

        logger.debug { "Send 'SICCT Card inserted Event' to Connector." }
        logger.debug { "Received 'cmdAPDU INTERNAL AUTHENTICATE' from Connector." }

        //sendReady(registerEgkPayload, session)
        sendMseAPDU(registerEgk.cardSessionId, session)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun sendMseAPDU(cardSessionId: String, session: Session) {
        val mseApdu = "002241A406840109800100".hexToByteArray()
        val mseMessage = GematikEnvelope(
            SendApdu(cardSessionId, mseApdu),
            mseCorrelationId,
            cardSessionId,
        )

        session.asyncRemote.sendObject(mseMessage) {
            if (it.exception != null) {
                logger.debug(it.exception) { "Unable to send message." }
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun sendInternalAuthenticateAPDU(cardSessionId: String, session: Session) {
        val randomBytes = Random.nextBytes(32).toHexString()
        val internalAuthApdu = "0088000020${randomBytes}00".hexToByteArray()
        val internalAuthMessage = GematikEnvelope(
            SendApdu(cardSessionId, internalAuthApdu),
            internalAuthCorrelationId,
            cardSessionId,
        )

        session.asyncRemote.sendObject(internalAuthMessage) {
            if (it.exception != null) {
                logger.debug(it.exception) { "Unable to send message." }
            }
        }
    }

    private fun sendRegisterEgkFinish(cardSessionId: String, session: Session) {
        val registerEgkFinish = RegisterEgkFinish(true)
        val correlationId = registerEgkCorrelationIds[cardSessionId]
        val gematikMessage = GematikEnvelope(registerEgkFinish, correlationId, cardSessionId)

        session.asyncRemote.sendObject(gematikMessage) {
            registerEgkCorrelationIds.remove(cardSessionId)
            if (it.exception != null) {
                logger.debug(it.exception) { "Unable to send message." }
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun handleApduResponse(apduResponseMessage: GematikEnvelope, session: Session) {
        val cardSessionId = apduResponseMessage.cardSessionId
        val correlationId = apduResponseMessage.correlationId
        val sendApduResponse = apduResponseMessage.payload

        if (cardSessionId == null || correlationId == null) {
            val errorMsg = "Didn't receive a cardSessionId or correlationId."
            logger.error { errorMsg }
            throw IllegalStateException(errorMsg)
        }

        if (sendApduResponse == null) {
            val errorMsg = "Payload is null."
            logger.error { errorMsg }
            sendError(session, errorMsg, cardSessionId, correlationId, 400)
            return
        }

        if (sendApduResponse !is SendApduResponse) {
            val errorMsg = "Gematik message is not from type: SendApduResponse."
            logger.error { errorMsg }
            sendError(session, errorMsg, cardSessionId, correlationId, 400)
            return
        }

        val apduResponse = sendApduResponse.response.toHexString()

        logger.debug { "Received APDU response $apduResponse payload for card session: '${cardSessionId}'." }
        logger.debug { "Send response of INTERNAL AUTHENTICATE to Connector." }

        when (correlationId) {
            mseCorrelationId -> sendInternalAuthenticateAPDU(cardSessionId, session)
            internalAuthCorrelationId -> sendRegisterEgkFinish(cardSessionId, session)
            else -> throw IllegalStateException("Received an unknown message with correlationId: $correlationId")
        }
    }

    private fun sendError(session: Session, errorMsg: String, cardSessionId: String, correlationId: String, status: Int) {
        val errorPayload = TasklistErrorPayload(
            cardSessionId = cardSessionId,
            status = status,
            errormessage = errorMsg,
        )

        val gematikEnvelope = GematikEnvelope(errorPayload, correlationId, cardSessionId)

        session.asyncRemote.sendObject(gematikEnvelope) {
            if (it.exception != null) {
                logger.debug(it.exception) { "Unable to send message." }
            }
        }
    }

    private fun getWebSocketId(session: Session) : String? {
        if (session.queryString == null) return null
        val queryString = if (session.queryString.startsWith("?")) session.queryString else "?${session.queryString}"
        val parameters = QueryStringDecoder(queryString).parameters()
        return parameters["token"]?.firstOrNull()
    }
}
