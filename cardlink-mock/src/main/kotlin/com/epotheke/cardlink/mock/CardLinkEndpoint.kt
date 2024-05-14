package com.epotheke.cardlink.mock

import com.epotheke.cardlink.mock.encoding.JsonEncoder
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeType
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.handler.codec.http.QueryStringDecoder
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.websocket.*
import jakarta.websocket.server.ServerEndpoint
import java.util.Base64
import java.util.UUID


private val logger = KotlinLogging.logger {}

@ApplicationScoped
@ServerEndpoint("/cardlink", subprotocols = ["cardlink"], encoders = [ JsonEncoder::class ])
class CardLinkEndpoint {

    @Inject
    lateinit var objMapper: ObjectMapper

    @Inject
    lateinit var smsSender: SpryngsmsSender

    @Inject
    lateinit var smsCodeHandler: SMSCodeHandler

    @OnOpen
    fun onOpen(session: Session, cfg: EndpointConfig) {
        val webSocketId = getWebSocketId(session)
        logger.debug { "New WebSocket connection with ID: $webSocketId." }
    }

    @OnClose 
    fun onClose(session:Session, reason: CloseReason) {
        val webSocketId = getWebSocketId(session)
        logger.debug { "WebSocket connection with ID: $webSocketId was closed." }
    }

    @OnError
    fun onError(session: Session, t: Throwable) {
        val webSocketId = getWebSocketId(session)
        logger.debug(t) { "An error occurred for WebSocket connection with ID: $webSocketId." }
    }

    @OnMessage
    fun onMessage(session: Session, data: String) {
        val payload = objMapper.readValue(data, JsonNode::class.java)

        if (! payload.nodeType.equals(JsonNodeType.ARRAY)) {
            // send error response payload
            logger.debug { "Payload is not from type array." }
        }

        val cardSessionId : String = if (payload.has(1)) payload.get(1).textValue() else throw IllegalArgumentException("No card-session-id provided by client.")
        val correlationId : String? = if (payload.has(2)) payload.get(2).textValue() else null

        logger.debug { "New incoming websocket message with cardSessionId '$cardSessionId' and correlationId '$correlationId'." }
        logger.debug { data }

        if (payload.has(0)) {
            val egkEnvelope = objMapper.treeToValue(payload.get(0), EgkEnvelope::class.java)

            logger.debug { "Incoming message with cardSessionId '$cardSessionId' from type '${egkEnvelope.type}'." }

            when (egkEnvelope.type) {
                EgkEnvelopeTypes.REQUEST_SMS_CODE -> {
                    handleRequestSmsCode(egkEnvelope.payload, cardSessionId, session)
                }
                EgkEnvelopeTypes.CONFIRM_SMS_CODE -> {
                    handleConfirmSmsCode(egkEnvelope.payload, cardSessionId, session)
                }
                EgkEnvelopeTypes.REGISTER_EGK_ENVELOPE_TYPE -> {
                    handleRegisterEgkPayload(egkEnvelope.payload, cardSessionId, session)
                }
                EgkEnvelopeTypes.SEND_APDU_RESPONSE_ENVELOPE -> {
                    handleApduResponse(egkEnvelope.payload, cardSessionId, session)
                }
                EgkEnvelopeTypes.TASK_LIST_ERROR_ENVELOPE -> {
                    logger.debug { "Received Tasklist Error Envelope message." }
                }
            }
        }
    }

    private fun handleConfirmSmsCode(payload: String?, cardSessionId: String, session: Session) {
        if (payload == null) {
            val errorMsg = "Payload is null."
            logger.error { errorMsg }
            sendError(session, errorMsg, cardSessionId, 400)
        }

        val confirmSmsCodePayload = objMapper.readValue(
            Base64.getDecoder().decode(payload),
            ConfirmSmsCodePayload::class.java
        )

        logger.debug { "Received 'confirmSmsCode' with sms code: '${confirmSmsCodePayload.smsCode}'." }

        val webSocketId = getWebSocketId(session)

        val correctSMSCode = if (webSocketId != null) {
            try {
                smsCodeHandler.checkSMSCode(webSocketId, confirmSmsCodePayload.smsCode)
            } catch (ex: MaxTriesReached) {
                logger.error { "Reached max tries for SMS-Code confirmation." }
                false
            }
        } else {
            logger.error { "Received wrong SMS-Code." }
            false
        }

        val resultCode = if (correctSMSCode) {
            "SUCCESS"
        } else {
            "FAILURE"
        }

        logger.debug { "Sending out 'confirmSmsCodeResponse' ..." }

        val confirmSmsCodeResponse = ConfirmSmsCodeResponsePayload(resultCode)
        val confirmSmsCodePayloadStr = objMapper.writeValueAsBytes(confirmSmsCodeResponse)
        val confirmSmsCodePayloadBase64 = Base64.getEncoder().encodeToString(confirmSmsCodePayloadStr)

        val confirmSmsCodeResponseEnvelope = EgkEnvelope(EgkEnvelopeTypes.CONFIRM_SMS_CODE_RESPONSE, confirmSmsCodePayloadBase64)
        val confirmSmsCodeResponseEnvelopeJson = objMapper.convertValue(confirmSmsCodeResponseEnvelope, JsonNode::class.java)

        val confirmSmsCodeResponseJson = objMapper.createArrayNode()
        confirmSmsCodeResponseJson.add(confirmSmsCodeResponseEnvelopeJson)
        confirmSmsCodeResponseJson.add(cardSessionId)
        confirmSmsCodeResponseJson.add(UUID.randomUUID().toString())

        session.asyncRemote.sendObject(confirmSmsCodeResponseJson) {
            if (it.exception != null) {
                logger.debug(it.exception) { "Unable to send message." }
            }
        }
    }

    private fun handleRequestSmsCode(payload: String?, cardSessionId: String, session: Session) {
        if (payload == null) {
            val errorMsg = "Payload is null."
            logger.error { errorMsg }
            sendError(session, errorMsg, cardSessionId, 400)
        }

        val requestSmsCodePayload = objMapper.readValue(
            Base64.getDecoder().decode(payload),
            RequestSmsCodePayload::class.java
        )

        val originalPhoneNumber = requestSmsCodePayload.phoneNumber
        val isGermanNumber = smsSender.isGermanPhoneNumber(originalPhoneNumber)

        if (! isGermanNumber) {
            val errorMsg = "Not a German phone number."
            logger.error { errorMsg }
            sendError(session, errorMsg, cardSessionId, 400)
        }

        val phoneNumber = smsSender.phoneNumberToInternationalFormat(originalPhoneNumber, "DE")

        logger.debug { "Received 'requestSmsCode' with senderId '${requestSmsCodePayload.senderId}' and phoneNumber '$originalPhoneNumber'." }
        logger.debug { "Got phone number: '$originalPhoneNumber' / International Form: $phoneNumber" }
        logger.debug { "Sending SMS out to '$phoneNumber'." }

        val webSocketId = getWebSocketId(session)

        if (webSocketId != null) {
            val smsCode = smsCodeHandler.createSMSCode(webSocketId)
            val smsCreateMessage = SMSCreateMessage(
                recipient = phoneNumber,
                smsCode = smsCode
            )
            smsSender.createMessage(smsCreateMessage)
        } else {
            val errorMsg = "Unable to get WebSocketID from query parameters."
            logger.error { errorMsg }
            sendError(session, errorMsg, cardSessionId, 400)
        }
    }

    fun handleRegisterEgkPayload(payload: String?, cardSessionId: String, session: Session) {
        if (payload == null) {
            val errorMsg = "Payload is null."
            logger.error { errorMsg }
            sendError(session, errorMsg, cardSessionId, 400)
        }

        val registerEgkPayload = objMapper.readValue(
            Base64.getDecoder().decode(payload),
            RegisterEgkPayload::class.java
        )

        logger.debug { "Send 'SICCT Card inserted Event' to Connector." }
        logger.debug { "Received 'cmdAPDU INTERNAL AUTHENTICATE' from Connector." }

        sendReady(registerEgkPayload, session)
        sendApdu(registerEgkPayload, session)
    }

    private fun sendReady(registerEgkPayload: RegisterEgkPayload, session: Session) {
        val readyEvelope = EgkEnvelope(EgkEnvelopeTypes.READY, null)
        val readyEnvelopeJson = objMapper.convertValue(readyEvelope, JsonNode::class.java)

        val readyJson = objMapper.createArrayNode()
        readyJson.add(readyEnvelopeJson)
        readyJson.add(registerEgkPayload.cardSessionId)
        readyJson.add(UUID.randomUUID().toString())

        session.asyncRemote.sendObject(readyJson) {
            if (it.exception != null) {
                logger.debug(it.exception) { "Unable to send message." }
            }
        }
    }

    private fun sendApdu(registerEgkPayload: RegisterEgkPayload, session: Session) {
        val sendApduPayload = SendApduPayload(registerEgkPayload.cardSessionId, "<BASE64_ENCODED_APDU>")
        val sendApduPayloadStr = objMapper.writeValueAsBytes(sendApduPayload)
        val sendApduPayloadBase64 = Base64.getEncoder().encodeToString(sendApduPayloadStr)

        val sendApduEnvelope = EgkEnvelope(EgkEnvelopeTypes.SEND_APDU_ENVELOPE, sendApduPayloadBase64)
        val sendApduEnvelopeJson = objMapper.convertValue(sendApduEnvelope, JsonNode::class.java)

        val sendApduJson = objMapper.createArrayNode()
        sendApduJson.add(sendApduEnvelopeJson)
        sendApduJson.add(registerEgkPayload.cardSessionId)
        sendApduJson.add(UUID.randomUUID().toString())

        session.asyncRemote.sendObject(sendApduJson) {
            if (it.exception != null) {
                logger.debug(it.exception) { "Unable to send message." }
            }
        }
    }

    private fun handleApduResponse(payload: String?, cardSessionId: String, session: Session) {
        if (payload == null) {
            val errorMsg = "Payload is null."
            logger.error { errorMsg }
            sendError(session, errorMsg, cardSessionId, 400)
        }

        val sendApduResponsePayload = objMapper.readValue(
            Base64.getDecoder().decode(payload),
            SendApduResponsePayload::class.java
        )

        logger.debug { "Received APDU response payload for card session: '${sendApduResponsePayload.cardSessionId}'." }
        logger.debug { "Send response of INTERNAL AUTHENTICATE to Connector." }
    }

    private fun sendError(session: Session, errorMsg: String, cardSessionId: String, status: Int) {
        val errorPayload = TasklistErrorPayload(
            cardSessionId = cardSessionId,
            status = status,
            errormessage = errorMsg,
        )
        val errorPayloadStr = objMapper.writeValueAsBytes(errorPayload)
        val errorPayloadBase64 = Base64.getEncoder().encodeToString(errorPayloadStr)

        val errorJson = objMapper.createArrayNode()
        errorJson.add(errorPayloadBase64)
        errorJson.add(cardSessionId)
        errorJson.add(UUID.randomUUID().toString())

        session.asyncRemote.sendObject(errorJson) {
            if (it.exception != null) {
                logger.debug(it.exception) { "Unable to send message." }
            }
        }
    }

    private fun getWebSocketId(session: Session) : String? {
        val queryString = if (session.queryString.startsWith("?")) session.queryString else "?${session.queryString}"
        val parameters = QueryStringDecoder(queryString).parameters()
        return parameters["token"]?.firstOrNull()
    }
}
