package com.epotheke.cardlink

import org.openecard.sal.sc.SmartcardDeviceConnection
import org.openecard.sal.sc.SmartcardSalSession

object SmartcardSalHelper {
    suspend fun connectFirstTerminalOnInsertCard(salSession: SmartcardSalSession): SmartcardDeviceConnection {
        val terminal =
            salSession.sal.terminals
                .list()
                .firstOrNull() ?: throw OtherNfcError("NFC stack could not be initialized")

        terminal.waitForCardPresent()
        return salSession.connect(terminal.name)
    }
}
