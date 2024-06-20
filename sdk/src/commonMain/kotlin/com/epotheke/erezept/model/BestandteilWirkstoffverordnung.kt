package com.epotheke.erezept.model


data class BestandteilWirkstoffverordnung(
    val type: String,
    val wirkstoffnummer: String,
    val wirkstoffname: String,
    val wirkstaerke: String,
    val wirkstaerkeneinheit: String,
    val additionalProperties: Map<String, Any>? = null,
)
