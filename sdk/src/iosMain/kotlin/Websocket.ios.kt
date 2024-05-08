import cocoapods.open_ecard.WebsocketProtocol
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
fun createWs(uri: String): WebsocketProtocol {
    return WebsocketIos(uri)
}

@OptIn(ExperimentalForeignApi::class)
class WebsocketIos(private val url: String) : NSObject(), WebsocketProtocol {
    override fun close(statusCode: Int, withReason: String?) {
        TODO("Not yet implemented")
    }

    override fun connect() {
        val client = HttpClient {
            install(WebSockets)
        }
        runBlocking {
            client.webSocket(method = HttpMethod.Get, host = "192.168.178.30", port = 8080, path = "/chat") {
                val conn = launch {
                    val othersMessage = incoming.receive() as? Frame.Text
                    othersMessage?.let {
                        println(othersMessage.readText())
                        send("Hello from clientImp")
                    }
                }
                conn.join()
            }
        }
        client.close()
        println("Connection closed. Goodbye!")
    }

    override fun getSubProtocol(): String? {
        TODO("Not yet implemented")
    }

    override fun getUrl(): String? {
        TODO("Not yet implemented")
    }

    override fun isClosed(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isFailed(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isOpen(): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeListener() {
        TODO("Not yet implemented")
    }

    override fun send(data: String?) {
        TODO("Not yet implemented")
    }

    override fun setListener(listener: NSObject?) {
        TODO("Not yet implemented")
    }
}
