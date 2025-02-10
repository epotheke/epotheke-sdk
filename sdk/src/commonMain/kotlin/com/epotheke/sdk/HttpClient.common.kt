package com.epotheke.sdk

import io.ktor.client.HttpClient

expect fun getHttpClient(tenantToken: String?): HttpClient
