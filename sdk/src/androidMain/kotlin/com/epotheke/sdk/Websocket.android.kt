package com.epotheke.sdk

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.plugins.websocket.cio.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import org.openecard.mobile.activation.Websocket
import org.openecard.mobile.activation.WebsocketListener
import org.openecard.mobile.activation.WebsocketException

private val log = KotlinLogging.logger {}

//ktor
//    mit cio engine
//kann auch ios kann alle
private class WiredWSListener(
    private val ws: Websocket,
    private val wsListener: WebsocketListener,
) {
    fun onOpen() = wsListener.onOpen(ws)
    fun onClose(code: Int, msg: String?) = wsListener.onClose(ws, code, msg)
    fun onError(error: String?) = wsListener.onError(ws, error)
    fun onText(text: String?) = wsListener.onText(ws, text)
}

class WebsocketAndroid(
    private val host: String,
    private val port: Int,
    private val path: String = "",
) : Websocket {

    private var wsListener: WiredWSListener? = null
    private val client: HttpClient = HttpClient {
        install(WebSockets)
    }

    private var wsSession: DefaultClientWebSocketSession? = null

    private suspend fun DefaultClientWebSocketSession.receiveLoop() {
        try {
            for (msg in incoming) {
                when {
                    (msg as? Frame.Text) != null -> {
                        wsListener?.onText(msg.readText())
                    }
                    (msg as? Frame.Close) != null -> {
                        val reason : CloseReason? = msg.readReason()
                        val code = reason?.code?.toInt() ?: CloseReason.Codes.INTERNAL_ERROR.code.toInt()
                        val reasonMsg = reason?.message ?: "No reason"

                        log.logOett { "Close Frame received" }

                        wsListener?.onClose(code, reasonMsg)
                    }
                    else -> {
                        wsListener?.onError("Invalid data received")
                    }
                }
            }
        } catch (e: Exception) {
            throw WebsocketException(e.message)
        }

    }

    /**
     * Set the listener for the websocket events.
     * This method replaces an existing listener, if one is already set.
     * @param listener the listener to set.
     */
    override fun setListener(wsListener: WebsocketListener) {
        this.wsListener = WiredWSListener(this, wsListener)
    }

    /**
     * Remove the listener for the websocket events, if one is set.
     */
    override fun removeListener() {
        this.wsListener = null
    }

    override fun getUrl(): String {
        return "ws://$host:$port/$path"
    }

    /**
     * Gets the selected subprotocol once the connection is established.
     * @return the selected subprotocol or null if no subprotocol was selected.
     */

    override fun getSubProtocol(): String? {
        TODO("Not yet implemented")
    }

    /**
     * Connect to the server.
     * This method can also be used to reestablish a lost connection.
     * @throws WebsocketException if the connection could not be established.
     */
    @Throws(WebsocketException::class)
    override fun connect() {
        runBlocking {
            wsSession = client.webSocketSession(
                method = HttpMethod.Get,
                host = host,
                port = port,
                path = path,
            )
            launch {
                wsListener?.onOpen()
                wsSession?.receiveLoop()
            }.join()
        }
    }

    /**
     * Get open state of the connection.
     * @return true if the connection is open, false otherwise.
     */
    override fun isOpen(): Boolean {
        return wsSession?.isActive ?: false
    }

    /**
     * Get failed state of the connection.
     * @return true if the connection is failed, false otherwise.
     */
    override fun isFailed(): Boolean {
        return !isOpen()
    }

    /**
     * Initiate the closing handshake.
     * @param statusCode the status code to send.
     * @param reason the reason for closing the connection, or null if none should be given.
     * @throws WebsocketException if the connection could not be closed.
     */

    @Throws(WebsocketException::class)
    override fun close(statusCode: Int, reason: String?) {
        runBlocking {
            try {
                wsSession?.outgoing?.send(Frame.Close(CloseReason(statusCode.toShort(), reason ?: "")))
            } catch (e: Exception) {
                throw WebsocketException(e.message)
            }
        }
    }

    /**
     * Send a text frame.
     * @param data the data to send.
     * @throws WebsocketException if the data could not be sent.
     */
    @Throws(WebsocketException::class)
    override fun send(data: String) {
        runBlocking {
            wsSession?.apply {
                if(!isOpen){throw WebsocketException("Websocket closed.")}
                send(data)
            }
        }
    }
}

