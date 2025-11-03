package com.epotheke

import com.epotheke.cardlink.OtherNfcError
import org.openecard.cif.bundled.EgkCif
import org.openecard.sal.sc.SmartcardDeviceConnection
import org.openecard.sal.sc.SmartcardSal
import org.openecard.sal.sc.recognition.CardRecognition
import org.openecard.sc.iface.CardChannel
import org.openecard.sc.iface.TerminalFactory
import org.openecard.sc.iface.withContextSuspend
import org.openecard.sc.pace.PaceFeatureSoftwareFactory

class SmartCardConnector(
    private val terminalFactory: TerminalFactory,
) {
    suspend fun connectCard(): SmartcardDeviceConnection =
        terminalFactory.load().withContextSuspend { terminals ->

            val recognizeAllCards =
                object : CardRecognition {
                    override fun recognizeCard(channel: CardChannel) = EgkCif.metadata.id
                }

            val terminal =
                terminals.list().firstOrNull()
                    ?: throw OtherNfcError("NFC stack could not be initialized")

            val session =
                SmartcardSal(
                    terminals,
                    setOf(EgkCif),
                    recognizeAllCards,
                    PaceFeatureSoftwareFactory(),
                ).startSession()
            terminal.waitForCardPresent()
            session.connect(terminal.name)
        }
}
