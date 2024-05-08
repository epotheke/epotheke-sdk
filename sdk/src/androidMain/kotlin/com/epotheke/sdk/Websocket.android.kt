package com.epotheke.sdk

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.openecard.mobile.activation.Websocket
import org.openecard.mobile.activation.WebsocketListener
import org.openecard.mobile.activation.WebsocketException

//ktor
//    mit cio engine
//kann auch ios kann alle

class WebsocketAndroid(private val url: String) : Websocket {

    private var wsListener: WebsocketListener? = null
    init {
        val that = this
    }

    /**
     * Set the listener for the websocket events.
     * This method replaces an existing listener, if one is already set.
     * @param listener the listener to set.
     */
    override fun setListener(wsListener: WebsocketListener?) {
        this.wsListener = wsListener
    }

    /**
     * Remove the listener for the websocket events, if one is set.
     */
    override fun removeListener() {
        this.wsListener = null
    }

    override fun getUrl(): String {
        return url
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
        val client = HttpClient {
            install(WebSockets)
        }
        runBlocking {
            client.webSocket(method = HttpMethod.Get, host = url, port = 8080, path = "/chat") {
                val conn = launch {
                    val othersMessage = incoming.receive() as? Frame.Text
                    othersMessage?.let {
                        val readMessage = othersMessage.readText()
                        wsListener?.let {

//                            it.onText(readMessage)

                        }
                        println(readMessage)
                        send("Hello from clientImp")
                    }
                }
                conn.join()
            }
        }
        client.close()
        println("Connection closed. Goodbye!")
    }
    /**
     * Get open state of the connection.
     * @return true if the connection is open, false otherwise.
     */
    override fun isOpen(): Boolean {
        TODO("Not yet implemented")
    }
    /**
     * Get failed state of the connection.
     * @return true if the connection is failed, false otherwise.
     */
    override fun isFailed(): Boolean {
        TODO("Not yet implemented")
    }

    /**
     * Initiate the closing handshake.
     * @param statusCode the status code to send.
     * @param reason the reason for closing the connection, or null if none should be given.
     * @throws WebsocketException if the connection could not be closed.
     */

    @Throws(WebsocketException::class)
    override fun close(statusCode: Int, reason: String?) {
        TODO("Not yet implemented")
    }
    /**
     * Send a text frame.
     * @param data the data to send.
     * @throws WebsocketException if the data could not be sent.
     */
    @Throws(WebsocketException::class)
    override fun send(data: String?) {
        TODO("Not yet implemented")
    }

}

