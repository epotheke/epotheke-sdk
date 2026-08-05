package com.epotheke.cardlink.core

import org.openecard.sal.sc.SmartcardDeviceConnection
import org.openecard.sal.sc.SmartcardSalSession

interface EgkInteraction {
    suspend fun onCanRequest(): String

    suspend fun onCanRetry(resultCode: CardCommunicationResultCode): String

    suspend fun requestCardInsertion(session: SmartcardSalSession): SmartcardDeviceConnection
}
