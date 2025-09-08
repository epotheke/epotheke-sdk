package com.epotheke.sdk

import com.epotheke.cardlink.CardlinkAuthenticationProtocol
import com.epotheke.erezept.protocol.PrescriptionProtocolImp
import org.openecard.sc.iface.TerminalFactory

class Epotheke(
    terminalFactory: TerminalFactory,
    serviceUrl: String,
    tenantToken: String?,
) {
    private val ws = WebsocketCommon(serviceUrl, tenantToken)

    val cardlinkAuthenticationProtocol =
        CardlinkAuthenticationProtocol(
            terminalFactory,
            ws,
        )
    val prescriptionProtocol = PrescriptionProtocolImp(ws)
}
