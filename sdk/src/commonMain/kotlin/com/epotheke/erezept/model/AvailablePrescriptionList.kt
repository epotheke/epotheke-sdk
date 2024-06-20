package com.epotheke.erezept.model


data class AvailablePrescriptionList(
    val type: String,
    val iccsn: String,
    val medicationSummaryList: List<Any>,
    val hint: String? = null,
    val additionalProperties: Map<String, Any>? = null,
)
