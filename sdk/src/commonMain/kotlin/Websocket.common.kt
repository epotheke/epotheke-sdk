import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*

private val log = KotlinLogging.logger {}

interface WiredWSListener {
    fun onOpen()
    fun onClose(code: Int, reason: String?)
    fun onError(error: String?)
    fun onText(msg: String?)
}

class WebsocketCommon(
    private val host: String,
    private val port: Int,
    private val path: String = "",
) {

    private var wsListener: WiredWSListener? = null
    private val client: HttpClient = HttpClient {
        install(WebSockets)
    }

    private var wsSession: DefaultClientWebSocketSession? = null

    private suspend fun DefaultClientWebSocketSession.receiveLoop() {
        for (msg in incoming) {
            log.debug { "Socket receive received frame with type: ${msg.frameType}" }
            when {
                (msg as? Frame.Text) != null -> {
                    wsListener?.onText(msg.readText())
                }

                (msg as? Frame.Close) != null -> {
                    val reason: CloseReason? = msg.readReason()
                    val code = reason?.code?.toInt() ?: CloseReason.Codes.INTERNAL_ERROR.code.toInt()
                    val reasonMsg = reason?.message ?: "No reason"

                    wsListener?.onClose(code, reasonMsg)
                }

                else -> {
                    log.warn { "Unhandled frame type received." }
                    wsListener?.onError("Invalid data received")
                }
            }
        }
    }

    /**
     * Set the listener for the websocket events.
     * This method replaces an existing listener, if one is already set.
     * @param listener the listener to set.
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

    fun getUrl(): String {
        return "ws://$host:$port/$path"
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
        runBlocking {
            wsSession = client.webSocketSession(
                method = HttpMethod.Get,
                host = host,
                port = port,
                path = path,
            )
            launch {
                log.debug { "Websocket connected to ${getUrl()}" }
                wsListener?.onOpen()
                wsSession?.receiveLoop()
            }.join()
        }
    }

    /**
     * Get open state of the connection.
     * @return true if the connection is open, false otherwise.
     */
    fun isOpen(): Boolean {
        return wsSession?.isActive ?: false
    }

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

    fun close(statusCode: Int, reason: String?) {
        log.debug { "Close was called. Close frame will be send." }
        runBlocking {
            wsSession?.close(
                CloseReason(
                    statusCode.toShort(),
                    reason ?: ""
                )
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

