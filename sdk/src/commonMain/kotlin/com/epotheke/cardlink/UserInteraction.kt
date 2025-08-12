package com.epotheke.cardlink

interface UserInteraction {
    suspend fun requestCardInsertion()

    suspend fun onCardRecognized()

//  suspend   fun requestCardInsertion(msgHandler: NFCOverlayMessageHandler?)

    suspend fun onCardInteractionComplete()

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

/*
class UserInteractionDelegate(
    private val externalUIImplementation: UserInteraction,
) : UserInteraction by externalUIImplementation {
    fun onCanRequest(block: (can: String) -> Unit) {
        externalUIImplementation.onCanRequest(
            object : ConfirmPasswordOperation {
                override fun confirmPassword(text: String) {
                    block(text)
                }
            },
        )
    }

    suspend fun onCanRequestSuspend() =
        suspendCoroutine { cont ->
            onCanRequest { can -> cont.resume(can) }
        }

    fun onCanRetry(
        resultCode: String,
        errorMessage: String,
        block: (can: String) -> Unit,
    ) {
        externalUIImplementation.onCanRetry(
            object : ConfirmPasswordOperation {
                override fun confirmPassword(text: String) {
                    block(text)
                }
            },
            resultCode,
            errorMessage,
        )
    }

    suspend fun onCanRetrySuspend(
        resultCode: String,
        errorMessage: String,
    ) = suspendCoroutine { cont ->
        onCanRetry(
            resultCode,
            errorMessage,
        ) { can -> cont.resume(can) }
    }

    fun onPhoneNumberRequest(block: (number: String) -> Unit) {
        externalUIImplementation.onPhoneNumberRequest(
            object : ConfirmTextOperation {
                override fun confirmText(text: String) {
                    block(text)
                }
            },
        )
    }

    suspend fun onPhoneNumberRequestSuspend() =
        suspendCoroutine { cont ->
            onPhoneNumberRequest { number -> cont.resume(number) }
        }

    fun onPhoneNumberRetry(
        resultCode: String,
        errorMessage: String,
        block: (number: String) -> Unit,
    ) {
        externalUIImplementation.onPhoneNumberRetry(
            object : ConfirmTextOperation {
                override fun confirmText(text: String) {
                    block(text)
                }
            },
            resultCode,
            errorMessage,
        )
    }

    suspend fun onPhoneNumberRetrySuspend(
        resultCode: String,
        errorMessage: String,
    ) = suspendCoroutine { cont ->
        onPhoneNumberRetry(
            resultCode,
            errorMessage,
        ) { number ->
            cont.resume(number)
        }
    }

    fun onSmsCodeRequest(block: (smsCode: String) -> Unit) {
        externalUIImplementation.onSmsCodeRequest(
            object : ConfirmPasswordOperation {
                override fun confirmPassword(text: String) {
                    block(text)
                }
            },
        )
    }

    suspend fun onSmsCodeRequestSuspend() =
        suspendCoroutine { cont ->
            onSmsCodeRequest { smsCode -> cont.resume(smsCode) }
        }

    fun onSmsCodeRetry(
        resultCode: String,
        errorMessage: String,
        block: (smsCode: String) -> Unit,
    ) {
        externalUIImplementation.onSmsCodeRetry(
            object : ConfirmPasswordOperation {
                override fun confirmPassword(text: String) {
                    block(text)
                }
            },
            resultCode,
            errorMessage,
        )
    }

    suspend fun onSmsCodeRetrySuspend(
        resultCode: String,
        errorMessage: String,
    ) = suspendCoroutine { cont ->
        onSmsCodeRetry(
            resultCode,
            errorMessage,
        ) { smsCode -> cont.resume(smsCode) }
    }
}

 */
