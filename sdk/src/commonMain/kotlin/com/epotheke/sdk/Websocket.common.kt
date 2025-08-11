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

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readReason
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.EOFException

private val log = KotlinLogging.logger {}

interface WiredWSListener {
    fun onOpen()

    fun onClose(
        code: Int,
        reason: String?,
    )

    fun onError(error: String)

    fun onText(msg: String)
}

open class WebsocketListenerCommon : ChannelDispatcher {
    private val channels = mutableListOf<Channel<String>>()

    override fun addProtocolChannel(channel: Channel<String>) {
        channels.add(channel)
    }

    fun onOpen() {
    }

    fun onClose(
        p1: Int,
        p2: String?,
    ) {
    }

    fun onError(p1: String) {
//        protos.map { p-> p.getErrorHandler()(p1) }
    }

    fun onText(p1: String) {
        runBlocking {
            channels.map { c ->
                c.send(p1)
            }
        }
    }
}

class WebsocketCommon(
    private var url: String,
    private var tenantToken: String?,
) {
    private var receiveJob: Job? = null
    private var wsListener: WiredWSListener? = null
    private val client: HttpClient = getHttpClient(tenantToken)

    private var wsSession: DefaultClientWebSocketSession? = null

    private suspend fun DefaultClientWebSocketSession.receiveLoop() {
        try {
            for (msg in incoming) {
                log.debug { "Socket receive received frame with type" }
                when (msg) {
                    is Frame.Text -> {
                        wsListener?.onText(msg.readText())
                    }

                    is Frame.Close -> {
                        // only used when websocket raw is used
                        val reason: CloseReason? = msg.readReason()
                        val code =
                            reason?.code?.toInt() ?: CloseReason.Codes.INTERNAL_ERROR.code
                                .toInt()
                        val reasonMsg = reason?.message ?: "No reason"

                        wsListener?.onClose(code, reasonMsg)
                    }

                    else -> {
                        log.warn { "Unhandled frame type received." }
                        wsListener?.onError("Invalid data received")
                    }
                }
            }
        } catch (e: EOFException) {
            log.debug { "Websocket channel closed by receiving EOFException." }
            val reasonMsg = e.message
            val code = CloseReason.Codes.NORMAL
            wsListener?.onClose(code.code.toInt(), reasonMsg)
        } catch (e: Exception) {
            log.debug(e) { "Exception during websocket receive." }
        }
    }

    /**
     * Set the listener for the websocket events.
     * This method replaces an existing listener, if one is already set.
     * @param wsListener the listener to set.
     */
    fun setListener(wsListener: WiredWSListener) {
        this.wsListener = wsListener
    }

    /**
     * Remove the listener for the websocket events, if one is set.
     */
    fun removeListener() {
        this.wsListener = null
    }

    fun getUrl(): String = this.url

    fun setUrl(url: String) {
        this.url = url
    }

    /**
     * Gets the selected subprotocol once the connection is established.
     * @return the selected subprotocol or null if no subprotocol was selected.
     */
    fun getSubProtocol(): String? {
        TODO("Not yet implemented")
    }

    /**
     * Connect to the server.
     * This method can also be used to reestablish a lost connection.
     */
    fun connect() {
        this.receiveJob?.cancel()
        runBlocking {
            val uri = Url(url)
            log.debug { "Connecting websocket to: $uri" }

            wsSession =
                client.webSocketSession(
                    method = HttpMethod.Get,
                    host = uri.host,
                    port = uri.port,
                    path = uri.fullPath,
                ) {
                    url {
                        url.protocol = URLProtocol.WSS
                        url.port = uri.port
                    }
                }

            log.debug { "Websocket connected" }
            wsListener?.onOpen()
        }

        this.receiveJob =
            CoroutineScope(Dispatchers.IO).launch {
                log.debug { "Entering websocket receive loop." }
                wsSession?.receiveLoop()
            }
    }

    /**
     * Get open state of the connection.
     * @return true if the connection is open, false otherwise.
     */
    fun isOpen(): Boolean = wsSession?.isActive ?: false

    /**
     * Get failed state of the connection.
     * @return true if the connection is failed, false otherwise.
     */
    fun isFailed(): Boolean {
        TODO("Do this correctly")
        //    return !isOpen()
    }

    /**
     * Initiate the closing handshake.
     * @param statusCode the status code to send.
     * @param reason the reason for closing the connection, or null if none should be given.
     */
    fun close(
        statusCode: Int,
        reason: String?,
    ) {
        log.debug { "Close was called. Close frame will be send." }
        runBlocking {
            wsSession?.close(
                CloseReason(
                    statusCode.toShort(),
                    reason ?: "",
                ),
            )
        }
    }

    /**
     * Send a text frame.
     * @param data the data to send.
     */
    fun send(data: String) {
        runBlocking {
            wsSession?.apply {
                send(data)
            }
        }
    }
}
