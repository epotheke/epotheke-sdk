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


private val logger = KotlinLogging.logger {}

@ApplicationScoped
@ServerEndpoint("/cardlink", subprotocols = ["cardlink"], encoders = [ JsonEncoder::class ])
class CardLinkEndpoint {

    @Inject
    lateinit var objMapper: ObjectMapper

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

        val cardSessionId : String? = if (payload.has(1)) payload.get(1).textValue() else null
        val correlationId : String? = if (payload.has(2)) payload.get(2).textValue() else null

        logger.debug { "New incoming websocket message with cardSessionId '$cardSessionId' and correlationId '$correlationId'." }

        if (payload.has(0)) {
            val egkEnvelope = objMapper.treeToValue(payload.get(0), EgkEnvelope::class.java)

            logger.debug { "Incoming message with cardSessionId '$cardSessionId' from type '${egkEnvelope.type}'." }

            when (egkEnvelope.type) {
                EgkEnvelopeTypes.REGISTER_EGK_ENVELOPE_TYPE -> {
                    handleRegisterEgkPayload(egkEnvelope.payload, session)
                }
                EgkEnvelopeTypes.SEND_APDU_RESPONSE_ENVELOPE -> {
                    handleApduResponse(egkEnvelope.payload, session)
                }
                EgkEnvelopeTypes.TASK_LIST_ERROR_ENVELOPE -> {
                    logger.debug { "Received Tasklist Error Envelope message." }
                }
            }
        }
    }

    fun handleRegisterEgkPayload(payload: String, session: Session) {
        val registerEgkPayload = objMapper.readValue(
            Base64.getDecoder().decode(payload),
            RegisterEgkPayload::class.java
        )

        logger.debug { "Send 'SICCT Card inserted Event' to Connector." }
        logger.debug { "Received 'cmdAPDU INTERNAL AUTHENTICATE' from Connector." }

        val sendApduPayload = SendApduPayload(registerEgkPayload.cardSessionId, "<BASE64_ENCODED_APDU>")
        val sendApduPayloadStr = objMapper.writeValueAsBytes(sendApduPayload)
        val sendApduPayloadBase64 = Base64.getEncoder().encodeToString(sendApduPayloadStr)

        val sendApduEnvelope = EgkEnvelope(EgkEnvelopeTypes.SEND_APDU_ENVELOPE, sendApduPayloadBase64)
        val sendApduEnvelopeJson = objMapper.convertValue(sendApduEnvelope, JsonNode::class.java)

        val sendApduJson = objMapper.createArrayNode()
        sendApduJson.add(sendApduEnvelopeJson)
        sendApduJson.add(registerEgkPayload.cardVersion)
        // TODO: set correlationId
        sendApduJson.add(registerEgkPayload.cardVersion)

        session.asyncRemote.sendObject(sendApduJson) {
            if (it.exception != null) {
                logger.debug(it.exception) { "Unable to send message." }
            }
        }
    }

    private fun handleApduResponse(payload: String, session: Session) {
        val sendApduResponsePayload = objMapper.readValue(
            Base64.getDecoder().decode(payload),
            SendApduResponsePayload::class.java
        )

        logger.debug { "Received APDU response payload for card session: '${sendApduResponsePayload.cardSessionId}'." }
        logger.debug { "Send response of INTERNAL AUTHENTICATE to Connector." }
    }

    private fun getWebSocketId(session: Session) : String? {
        val queryString = if (session.queryString.startsWith("?")) session.queryString else "?${session.queryString}"
        val parameters = QueryStringDecoder(queryString).parameters()
        return parameters["token"]?.firstOrNull()
    }
}
