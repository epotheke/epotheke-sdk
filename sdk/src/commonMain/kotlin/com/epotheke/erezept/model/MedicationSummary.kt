package com.epotheke.erezept.model


data class MedicationSummary(
    val type: String? = null,
    val index: Int,
    val medication: Any,
    val additionalProperties: Map<String, Any>? = null,
)
