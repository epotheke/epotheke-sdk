package com.epotheke.cardlink.core

import com.epotheke.cardlink.core.Egk.Companion.CAN_LEN

enum class CardCommunicationResultCode(
    val msg: String?,
) {
    CAN_INCORRECT("Provided CAN was not correct."),

    CAN_EMPTY("Empty CAN provided."),
    CAN_LEN_WRONG("Can must have $CAN_LEN digits."),
    CAN_NOT_NUMERIC("Provided CAN is not numeric."),

    CARD_ERROR("Error communicating with card."),
}
