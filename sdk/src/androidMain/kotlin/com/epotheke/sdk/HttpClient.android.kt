package com.epotheke.sdk

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import kotlin.time.Duration.Companion.seconds

actual fun getHttpClient(tenantToken: String?): HttpClient =
    HttpClient(OkHttp) {
        install(WebSockets) {
            pingInterval = 15.seconds
        }
        tenantToken?.let {
            install(Auth) {
                bearer {
                    loadTokens {
                        BearerTokens(it, "")
                    }
                }
            }
        }
    }
