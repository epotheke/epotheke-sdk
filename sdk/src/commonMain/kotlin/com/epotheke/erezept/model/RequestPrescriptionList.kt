package com.epotheke.erezept.model


data class RequestPrescriptionList(
    val type: String,
    val iccsNs: List<Any>? = null,
    val messageId: String,
    val additionalProperties: Map<String, Any>? = null,
)
