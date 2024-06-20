package com.epotheke.erezept.model


data class GenericErrorMessage(
    val type: String,
    val errorCode: GenericErrorResultType,
    val errorMessage: String,
    val messageId: String,
    val correlationId: String? = null,
    val additionalProperties: Map<String, Any>? = null,
)
