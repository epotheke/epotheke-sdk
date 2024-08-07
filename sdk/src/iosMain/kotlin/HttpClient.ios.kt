import io.ktor.client.*
import io.ktor.client.plugins.websocket.*

actual fun getHttpClient(): HttpClient {
    return HttpClient() {
        install(WebSockets) {
            pingInterval = 15_000
        }
    }
}
