package com.epotheke.cardlink

enum class CardlinkProcessStepKind {
    READ_PERSONAL_DATA,
    READ_INSURER_DATA,
    READ_CARD_DATA,
    SEND_REGISTER_EGK,
    HANDLE_REMOTE_APDU,
}

class CardlinkProcessStep(
    val kind: CardlinkProcessStepKind,
    val detail: String? = null,
)

interface ProgressNotification {
    suspend fun onProgress(step: CardlinkProcessStep)
}
