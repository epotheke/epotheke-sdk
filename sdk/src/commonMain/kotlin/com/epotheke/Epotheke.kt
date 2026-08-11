package com.epotheke

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.epotheke.cardlink.CardLinkAuthenticationProtocol
import com.epotheke.prescription.PrescriptionProtocolImp
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import org.openecard.cif.bundled.EgkCif
import org.openecard.cif.definition.CardInfoDefinition
import org.openecard.sal.sc.SmartcardSal
import org.openecard.sal.sc.SmartcardSalSession
import org.openecard.sal.sc.recognition.CardRecognition
import org.openecard.sc.iface.CardChannel
import org.openecard.sc.iface.TerminalFactory
import org.openecard.sc.pace.PaceFeatureSoftwareFactory

private val logger = KotlinLogging.logger {}

class Epotheke internal constructor(
    serviceUrl: String,
    tenantToken: String?,
    salSession: SmartcardSalSession,
    wsSessionId: String? = null,
) : AutoCloseable {
    private val ws = Websocket(serviceUrl, tenantToken, wsSessionId)

    val cardLinkAuthenticationProtocol =
        CardLinkAuthenticationProtocol(
            ws,
            salSession,
        )
    val prescriptionProtocol = PrescriptionProtocolImp(ws)

    @OptIn(InternalCoroutinesApi::class)
    override fun close() {
        CoroutineScope(Dispatchers.IO).launch {
            logger.debug { "Closing Epotheke (the websocket)" }
            ws.close(1000, "Client stopped.")
        }
    }

    companion object {
        @DefaultArgumentInterop.Enabled
        fun createEpothekeService(
            terminalFactory: TerminalFactory,
            serviceUrl: String,
            tenantToken: String?,
            wsSessionId: String? = null,
            cifs: Set<CardInfoDefinition> = setOf(EgkCif),
            recognition: CardRecognition = egkRecognition,
        ): Epotheke {
            val salSession =
                SmartcardSal(
                    terminalFactory.load(),
                    cifs,
                    recognition,
                    PaceFeatureSoftwareFactory(),
                ).startSession()
            return Epotheke(serviceUrl, tenantToken, salSession, wsSessionId)
        }

        private val egkRecognition =
            object : CardRecognition {
                override fun recognizeCard(channel: CardChannel) = EgkCif.metadata.id
            }
    }
}
