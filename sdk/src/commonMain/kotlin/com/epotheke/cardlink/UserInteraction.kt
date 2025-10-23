package com.epotheke.cardlink

interface UserInteraction {
    suspend fun requestCardInsertion()

    suspend fun onCardRecognized()

    suspend fun onCanRequest(): String

    suspend fun onCanRetry(resultCode: CardCommunicationResultCode): String

    suspend fun onPhoneNumberRequest(): String

    suspend fun onPhoneNumberRetry(
        resultCode: ResultCode,
        msg: String?,
    ): String

    suspend fun onTanRequest(): String

    suspend fun onTanRetry(
        resultCode: ResultCode,
        msg: String?,
    ): String
}
