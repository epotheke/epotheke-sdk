package com.epotheke.erezept.model


data class MedicationIngredient(
    val type: String,
    val kategorie: String,
    val impfstoff: Boolean,
    val normgroesse: String? = null,
    val darreichungsform: String,
    val packungsgroesseNachMenge: String? = null,
    val einheit: String? = null,
    val packungsgroesseNachNBezeichnung: String? = null,
    val listeBestandteilWirkstoffverordnung: List<Any>,
    val additionalProperties: Map<String, Any>? = null,
)
