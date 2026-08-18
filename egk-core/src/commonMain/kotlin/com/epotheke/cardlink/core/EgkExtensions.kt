package com.epotheke.cardlink.core

import com.epotheke.cardlink.core.Egk.Companion.CAN_LEN
import org.openecard.sal.sc.SmartcardSalSession
import org.openecard.sc.apdu.ApduProcessingError
import org.openecard.sc.iface.feature.PaceError

object EgkExtensions {
    typealias EgkProcessingBlock<T> = suspend Egk.() -> T

    /**
     * Saves correctly entered CANs for SAL sessionIds.
     * The CAN is saved once it is entered successfully.
     * If the process is then restarted, the CAN is tried again omitting the interaction call for requesting the CAN.
     */
    private val correctCansPerSession = mutableMapOf<String, String>()

    suspend fun <T> SmartcardSalSession.withAuthenticatedEgk(
        interaction: EgkInteraction,
        block: EgkProcessingBlock<T>,
    ): T {
        val salSession = this
        return repeatIfWrongCan(interaction) { can ->
            val connection = interaction.requestCardInsertion(salSession)
            Egk(connection).run {
                authenticatePace(can)
                correctCansPerSession[sessionId] = can
                val result = block()
                correctCansPerSession.remove(sessionId)
                result
            }
        }
    }

    private suspend fun <T> SmartcardSalSession.repeatIfWrongCan(
        interaction: EgkInteraction,
        block: suspend (can: String) -> T,
    ): T = doRepetition(null, interaction, block)

    private suspend fun <T> SmartcardSalSession.doRepetition(
        cardCommunicationResult: CardCommunicationResultCode?,
        interaction: EgkInteraction,
        block: suspend (can: String) -> T,
    ): T {
        val salSession = this
        try {
            // TODO: rewrite to put cleanup into finalize or similar
            val can = correctCansPerSession[sessionId] ?: getCheckedCan(interaction, cardCommunicationResult)
            salSession.initializeStack()
            val result = block(can)
            salSession.shutdownStack()
            return result
        } catch (e: Exception) {
            salSession.shutdownStack()
            when (e) {
                is ApduProcessingError,
                -> {
                    return doRepetition(
                        CardCommunicationResultCode.CARD_ERROR,
                        interaction,
                        block,
                    )
                }

                is PaceError,
                -> {
                    correctCansPerSession.remove(sessionId)
                    return doRepetition(
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
}
