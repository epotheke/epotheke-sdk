import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*

actual fun getHttpClient(tenantToken: String?): HttpClient {

    return HttpClient(OkHttp) {
        install(WebSockets) {
            pingInterval = 15_000
        }
        tenantToken?.let{
            install(Auth) {
                bearer {
                    loadTokens {
                        BearerTokens( it, "" )
                    }
                }
            }
        }
    }
}
