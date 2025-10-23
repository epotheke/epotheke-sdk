package com.epotheke.cardlink

enum class CardCommunicationResultCode(
    val msg: String?,
) {
    CAN_INCORRECT("Provided CAN was not correct."),

    CAN_EMPTY("Empty CAN provided."),
    CAN_TOO_LONG("Can too long."),
    CAN_NOT_NUMERIC("Provided CAN is not numeric."),
}

// TODO error happend on client side if can too short
