package com.epotheke.sdk

import org.openecard.mobile.activation.ServiceErrorResponse

interface SdkErrorHandler {
    fun onError(error: ServiceErrorResponse)
}
