package com.epotheke.cardlink

sealed class CardLinkClientError(
    override val message: String,
    cause: Exception? = null,
) : Exception(cause)

class CanStepInterrupted(
    message: String = "The CAN based channel establishment was interrupted.",
    cause: Exception? = null,
) : CardLinkClientError(message, cause)

class CardRemoved(
    message: String = "The card was removed or connection was interrupted.",
    cause: Exception? = null,
) : CardLinkClientError(message, cause)

class CardInsufficient(
    message: String = "The provided card is not sufficient.",
    cause: Exception? = null,
) : CardLinkClientError(message, cause)

class OtherPaceError(
    message: String = "Error during PACE.",
    cause: Exception? = null,
) : CardLinkClientError(message, cause)

class OtherNfcError(
    message: String = "Error during NFC communication.",
    cause: Exception? = null,
) : CardLinkClientError(message, cause)

class Timeout(
    message: String = "A timeout happened.",
    cause: Exception? = null,
) : CardLinkClientError(message, cause)

class OtherClientError(
    message: String = "An error happened on client side.",
    cause: Exception? = null,
) : CardLinkClientError(message, cause)
