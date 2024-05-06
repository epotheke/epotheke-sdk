package com.epotheke.cardlink.mock


data class EgkEnvelope(
    val type: String,
    val payload: String?,
)

object EgkEnvelopeTypes {
    const val REQUEST_SMS_CODE = "requestSMSCode"
    const val CONFIRM_SMS_CODE = "confirmSMSCode"
    const val CONFIRM_SMS_CODE_RESPONSE = "confirmSMSCodeResponse"
    const val REGISTER_EGK_ENVELOPE_TYPE = "registerEGK"
    const val TASK_LIST_ERROR_ENVELOPE = "receiveTasklistError"
    const val SEND_APDU_ENVELOPE = "sendAPDU"
    const val READY = "ready"
    const val SEND_APDU_RESPONSE_ENVELOPE = "sendAPDUResponse"
}

data class RequestSmsCodePayload(
    val senderId: String,
    val phoneNumber: String,
)

data class ConfirmSmsCodePayload(
    val smsCode: String,
)

data class ConfirmSmsCodeResponsePayload(
    val result: String,
)

data class RegisterEgkPayload(
    val cardSessionId: String,
    val gdo: String,
    val cardVersion: String,
    val x509AuthRSA: String? = null,
    val x509AuthECC: String,
    val cvcAuth: String,
    val cvcCA: String,
    val atr: String,
)

data class SendApduPayload(
    val cardSessionId: String,
    val apdu: String,
)

data class SendApduResponsePayload(
    val cardSessionId: String,
    val response: String,
)

data class TasklistErrorPayload(
    val cardSessionId: String,
    val status: Int,
    val tistatus: String? = null,
    val rootcause: String? = null,
    val errormessage: String? = null,
)
