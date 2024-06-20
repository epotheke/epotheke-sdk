package com.epotheke.erezept.model


data class SelectedPrescriptionList(
    val type: String,
    val iccsn: String,
    val medicationIndexList: List<Any>,
    val supplyOptionsType: SupplyOptionsType,
    val name: String? = null,
    val address: List<String>? = null,
    val hint: String? = null,
    val phone: String? = null,
    val messageId: String,
    val additionalProperties: Map<String, Any>? = null,
)
