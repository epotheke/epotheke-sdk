import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.websocket.*

actual fun getHttpClient(tenantToken: String?): HttpClient {
    return HttpClient() {
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
