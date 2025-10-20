package com.epotheke.cardlink

interface UserInteraction {
    suspend fun requestCardInsertion()

    suspend fun onCardRecognized()

    suspend fun onCardInsufficient()

    suspend fun onCardRemoved()

    suspend fun onCanRequest(): String

    suspend fun onCanRetry(
        resultCode: CardLinkErrorCodes.ClientCodes,
        errorMessage: String?,
    ): String

    suspend fun onPhoneNumberRequest(): String

    suspend fun onPhoneNumberRetry(
        resultCode: ResultCode,
        errorMessage: String?,
    ): String

    suspend fun onTanRequest(): String

    suspend fun onTanRetry(
        resultCode: ResultCode,
        errorMessage: String?,
    ): String
}
