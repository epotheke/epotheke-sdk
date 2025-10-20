package com.epotheke

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import kotlin.time.Duration.Companion.seconds

fun getHttpClient(tenantToken: String?): HttpClient =
    HttpClient {
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
