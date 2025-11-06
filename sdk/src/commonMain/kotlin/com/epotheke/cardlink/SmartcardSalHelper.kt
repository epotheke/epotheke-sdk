package com.epotheke.cardlink

import io.ktor.utils.io.CancellationException
import org.openecard.sal.sc.SmartcardDeviceConnection
import org.openecard.sal.sc.SmartcardSalSession

object SmartcardSalHelper {
    @Throws(
        CardLinkClientError::class,
        CancellationException::class,
    )
    suspend fun connectFirstTerminalOnInsertCard(salSession: SmartcardSalSession): SmartcardDeviceConnection =
        try {
            val terminal =
                salSession.sal.terminals
                    .list()
                    .firstOrNull() ?: throw OtherNfcError("NFC stack could not be initialized")

            terminal.waitForCardPresent()
            salSession.connect(terminal.name)
        } catch (e: Exception) {
            throw OtherNfcError(
                cause = e,
            )
        }
}
