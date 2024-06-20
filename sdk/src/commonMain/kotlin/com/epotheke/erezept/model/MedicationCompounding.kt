package com.epotheke.erezept.model


data class MedicationCompounding(
    val type: String,
    val kategorie: String,
    val impfstoff: Boolean,
    val herstellungsanweisung: String,
    val verpackung: String? = null,
    val rezepturname: String,
    val darreichungsform: String,
    val gesamtmenge: String,
    val einheit: String? = null,
    val listeBestandteilRezepturverordnung: List<Any>,
    val additionalProperties: Map<String, Any>? = null,
)
