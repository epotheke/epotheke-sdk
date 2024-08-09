import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*

actual fun getHttpClient(): HttpClient {
    return HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 15_000
        }
        engine {
            endpoint {
                keepAliveTime = 15_000
                socketTimeout = 120_000
            }
        }
    }
}
