package com.epotheke.erezept.model


data class AvailablePrescriptionLists(
    val type: String,
    val availablePrescriptionLists: List<Any>,
    val messageId: String,
    val correlationId: String,
    val additionalProperties: Map<String, Any>? = null,
)
