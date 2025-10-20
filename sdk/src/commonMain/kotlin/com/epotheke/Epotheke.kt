package com.epotheke

import com.epotheke.cardlink.CardlinkAuthenticationProtocol
import com.epotheke.prescription.protocol.PrescriptionProtocolImp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import org.openecard.sc.iface.TerminalFactory

class Epotheke(
    terminalFactory: TerminalFactory,
    serviceUrl: String,
    tenantToken: String?,
    wsSessionId: String? = null,
) : AutoCloseable {
    private val ws = Websocket(serviceUrl, tenantToken, wsSessionId)

    val cardlinkAuthenticationProtocol =
        CardlinkAuthenticationProtocol(
            terminalFactory,
            ws,
        )
    val prescriptionProtocol = PrescriptionProtocolImp(ws)

    @OptIn(InternalCoroutinesApi::class)
    override fun close() {
        CoroutineScope(Dispatchers.IO).launch {
            ws.close(1000, "Client stopped.")
        }
    }
}
