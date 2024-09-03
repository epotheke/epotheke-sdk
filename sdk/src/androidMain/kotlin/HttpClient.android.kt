import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.engine.okhttp.*

actual fun getHttpClient(): HttpClient {

    return HttpClient(OkHttp) {
        install(WebSockets) {
            pingInterval = 15_000
        }
    }
}
