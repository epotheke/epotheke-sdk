package com.epotheke.cardlink.core

import com.epotheke.cardlink.core.Egk.Companion.CAN_LEN
import org.openecard.sal.sc.SmartcardSalSession
import org.openecard.sc.apdu.ApduProcessingError
import org.openecard.sc.iface.feature.PaceError

suspend fun SmartcardSalSession.withAuthenticatedEgk(
    interaction: EgkInteraction,
    block: suspend Egk.() -> Unit,
) {
    val salSession = this
    repeatIfWrongCan(interaction) { can ->
        val connection = interaction.requestCardInsertion(salSession)
        Egk(connection).run {
            authenticatePace(can)
            block()
        }
    }
}

private suspend fun SmartcardSalSession.repeatIfWrongCan(
    interaction: EgkInteraction,
    block: suspend (can: String) -> Unit,
) {
    doRepetition(null, interaction, block)
}

private suspend fun SmartcardSalSession.doRepetition(
    cardCommunicationResult: CardCommunicationResultCode?,
    interaction: EgkInteraction,
    block: suspend (can: String) -> Unit,
) {
    val salSession = this
    try {
        val can = getCheckedCan(interaction, cardCommunicationResult)
        salSession.initializeStack()
        block(can)
        salSession.shutdownStack()
    } catch (e: Exception) {
        salSession.shutdownStack()
        when (e) {
            is ApduProcessingError,
            -> {
                doRepetition(
                    CardCommunicationResultCode.CARD_ERROR,
                    interaction,
                    block,
                )
            }

            is PaceError,
            -> {
                doRepetition(
                    CardCommunicationResultCode.CAN_INCORRECT,
                    interaction,
                    block,
                )
            }

            else -> {
                throw e
            }
        }
    }
}

private suspend fun getCheckedCan(
    interaction: EgkInteraction,
    lastResultCode: CardCommunicationResultCode?,
): String {
    val can =
        if (lastResultCode == null) {
            interaction.onCanRequest()
        } else {
            interaction.onCanRetry(lastResultCode)
        }
    return if (can.isBlank()) {
        getCheckedCan(interaction, CardCommunicationResultCode.CAN_EMPTY)
    } else if (can.length != CAN_LEN) {
        getCheckedCan(interaction, CardCommunicationResultCode.CAN_LEN_WRONG)
    } else if (can.toIntOrNull() == null) {
        getCheckedCan(interaction, CardCommunicationResultCode.CAN_NOT_NUMERIC)
    } else {
        can
    }
}
