package com.epotheke.cardlink

import com.epotheke.cardlink.core.EgkInteraction

interface UserInteraction : EgkInteraction {
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
