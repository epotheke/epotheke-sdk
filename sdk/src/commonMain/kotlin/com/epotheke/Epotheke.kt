package com.epotheke

import com.epotheke.cardlink.CardLinkAuthenticationProtocol
import com.epotheke.prescription.PrescriptionProtocolImp
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger {}

class Epotheke(
    serviceUrl: String,
    tenantToken: String?,
    wsSessionId: String? = null,
) : AutoCloseable {
    private val ws = Websocket(serviceUrl, tenantToken, wsSessionId)

    val cardLinkAuthenticationProtocol =
        CardLinkAuthenticationProtocol(
            ws,
        )
    val prescriptionProtocol = PrescriptionProtocolImp(ws)

    @OptIn(InternalCoroutinesApi::class)
    override fun close() {
        CoroutineScope(Dispatchers.IO).launch {
            logger.debug { "Closing Epotheke (the websocket)" }
            ws.close(1000, "Client stopped.")
        }
    }
}
