package com.epotheke.sdk

import com.epotheke.cardlink.CardlinkAuthenticationProtocol
import com.epotheke.erezept.protocol.PrescriptionProtocolImp
import org.openecard.sc.iface.TerminalFactory

class Epotheke(
    terminalFactory: TerminalFactory,
    serviceUrl: String,
    tenantToken: String?,
    wsSessionId: String? = null,
) {
    private val ws = WebsocketCommon(serviceUrl, tenantToken, wsSessionId)

    val cardlinkAuthenticationProtocol =
        CardlinkAuthenticationProtocol(
            terminalFactory,
            ws,
        )
    val prescriptionProtocol = PrescriptionProtocolImp(ws)
}
