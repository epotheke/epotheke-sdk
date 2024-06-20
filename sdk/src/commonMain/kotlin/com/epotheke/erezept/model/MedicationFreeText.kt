package com.epotheke.erezept.model


data class MedicationFreeText(
    val type: String,
    val kategorie: String,
    val impfstoff: Boolean,
    val freitextverordnung: String,
    val darreichungsform: String,
    val additionalProperties: Map<String, Any>? = null,
)
