package com.epotheke.cardlink.mock

import jakarta.enterprise.context.ApplicationScoped
import jakarta.websocket.CloseReason
import jakarta.websocket.EndpointConfig
import jakarta.websocket.OnClose
import jakarta.websocket.OnError
import jakarta.websocket.OnMessage
import jakarta.websocket.OnOpen
import jakarta.websocket.Session
import jakarta.websocket.server.ServerEndpoint

@ApplicationScoped
@ServerEndpoint("/cardlink", subprotocols = ["cardlink"])
class CardLinkEndpoint {
    @OnOpen
    fun onOpen(session: Session, cfg: EndpointConfig) {

    }

    @OnClose
    fun onClose(session:Session, reason: CloseReason) {

    }

    @OnError
    fun onError(session: Session, t: Throwable) {

    }

    @OnMessage
    fun onMessage(session: Session, data: String) {

    }
}
