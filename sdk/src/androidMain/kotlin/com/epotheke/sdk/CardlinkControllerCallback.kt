package com.epotheke.sdk

import org.openecard.mobile.activation.ActivationResult

interface CardlinkControllerCallback {
    fun onStarted()
    fun onAuthenticationCompletion(p0: ActivationResult?, cardlinkProtocols: Set<CardLinkProtocol>)
}
