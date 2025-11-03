package com.epotheke.cardlink

import org.openecard.sal.sc.SmartcardDeviceConnection

interface UserInteraction {
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

    suspend fun onCanRequest(): String

    suspend fun onCanRetry(resultCode: CardCommunicationResultCode): String

    suspend fun requestCardInsertion(): SmartcardDeviceConnection
}
