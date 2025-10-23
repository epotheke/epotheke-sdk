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

package com.epotheke

import com.epotheke.cardlink.GematikEnvelope
import com.epotheke.cardlink.SessionInformation
import com.epotheke.prescription.prescriptionJsonFormatter
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
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.io.EOFException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger {}

class Websocket(
    private var url: String,
    tenantToken: String?,
    private var wsSessionId: String? = null,
) {
    private var receiveJob: Job? = null

    fun addProtocol(protocol: CardLinkProtocol) {
        protocols.add(protocol)
    }

    private suspend fun handleMessageWithProtocols(msg: String) {
        val handlers =
            protocols.mapNotNull { p ->
                p.messageHandler(msg)
            }
        if (handlers.isEmpty()) {
            log.warn { "Dropping message, since no protocol can handle it." }
            log.debug { "Message was $msg" }
        }
        handlers.forEach { it.invoke() }
    }

    /**
     * We add a small protocol to the protocol list to allow overriding wsSessionId if needed
     */
    private val wsSessionIdProtocol =
        object : CardLinkProtocol {
            override fun messageHandler(msg: String): (suspend () -> Unit)? =
                try {
                    when (val payload = prescriptionJsonFormatter.decodeFromString<GematikEnvelope>(msg).payload) {
                        is SessionInformation -> {
                            {
                                if (wsSessionId != null && wsSessionId != payload.webSocketId) {
                                    log.warn {
                                        "Websocket session id overwritten by service to: ${payload.webSocketId}"
                                    }
                                }
                                wsSessionId = payload.webSocketId
                            }
                        }
                        else -> {
                            null
                        }
                    }
                } catch (_: Exception) {
                    null
                }
        }

    // Not that the protocols is initialised containing the wsSessionIdProtocol
    private val protocols = mutableListOf<CardLinkProtocol>(wsSessionIdProtocol)

    private val client: HttpClient = getHttpClient(tenantToken)
    private val wsSessionContext = Dispatchers.IO.limitedParallelism(1)
    private var wsSession: DefaultClientWebSocketSession? = null

    private suspend fun DefaultClientWebSocketSession.receiveLoop() {
        try {
            for (msg in incoming) {
                when (msg) {
                    is Frame.Text -> {
                        val read = msg.readText()
                        handleMessageWithProtocols(read)
                    }

                    is Frame.Close -> {
                        // only used when websocket raw is used
                        val reason: CloseReason? = msg.readReason()
                        log.warn { "Websocket received close frame $reason." }
                    }

                    else -> {
                        log.warn { "Unhandled frame type received." }
                    }
                }
            }
        } catch (e: EOFException) {
            log.error(e) { "Websocket closed by EOFException." }
        } catch (e: Exception) {
            log.error(e) { "Exception during websocket receive." }
        }
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
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun connect() =
        withContext(wsSessionContext) {
            receiveJob?.cancel()
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
                        protocol = URLProtocol.WSS
                        port = uri.port
                        wsSessionId?.let {
                            parameters.append("token", it)
                        }
                    }
                }

            log.debug { "Websocket connected" }

            receiveJob =
                CoroutineScope(Dispatchers.IO).launch {
                    log.debug { "Entering websocket receive loop." }
                    wsSession?.receiveLoop()
                }
        }

    suspend fun connectWithTimeout(duration: Duration = 10.seconds) {
        withTimeout(duration) {
            connect()
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
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun close(
        statusCode: Int,
        reason: String?,
    ) = withContext(wsSessionContext) {
        log.debug { "Close was called. Close frame will be send." }
        val sessToClose = wsSession
        wsSession = null
        sessToClose?.close(
            CloseReason(
                statusCode.toShort(),
                reason ?: "",
            ),
        )
    }

    /**
     * Send a text frame.
     * @param data the data to send.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun send(
        data: String,
        reconnectIfNeeded: Boolean = true,
        reconnectTimeoutDuration: Duration? = 10.seconds,
    ) = withContext(wsSessionContext) {
        val open = isOpen()
        if (reconnectIfNeeded && !open) {
            if (reconnectTimeoutDuration != null) {
                connectWithTimeout(reconnectTimeoutDuration)
            } else {
                connect()
            }
        }
        wsSession?.send(data)
    }
}
