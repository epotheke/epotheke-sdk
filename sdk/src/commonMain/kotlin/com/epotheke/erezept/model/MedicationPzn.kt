package com.epotheke.erezept.model


data class MedicationPzn(
    val type: String,
    val kategorie: String,
    val impfstoff: Boolean,
    val normgroesse: String? = null,
    val pzn: String,
    val handelsname: String,
    val darreichungsform: String,
    val packungsgroesseNachMenge: String? = null,
    val einheit: String? = null,
    val packungsgroesseNachNBezeichnung: String? = null,
    val additionalProperties: Map<String, Any>? = null,
)
