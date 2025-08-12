package com.epotheke.cardlink

class CardlinkAuthenticationClientException(
    val code: CardLinkErrorCodes.ClientCodes,
    msg: String,
    cause: Exception? = null,
) : Exception(msg, cause)

class CardlinkAuthenticationException(
    val code: CardLinkErrorCodes.CardLinkCodes,
    msg: String,
    cause: Exception? = null,
) : Exception(msg, cause)
