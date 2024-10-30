/****************************************************************************
 * Copyright (C) 2024 ecsec GmbH.
 * All rights reserved.
 * Contact: ecsec GmbH (info@ecsec.de)
 *
 * This file is part of the epotheke SDK.
 *
 * GNU General Public License Usage
 * This file may be used under the terms of the GNU General Public
 * License version 3.0 as published by the Free Software Foundation
 * and appearing in the file LICENSE.GPL included in the packaging of
 * this file. Please review the following information to ensure the
 * GNU General Public License version 3.0 requirements will be met:
 * http://www.gnu.org/copyleft/gpl.html.
 *
 * Other Usage
 * Alternatively, this file may be used in accordance with the terms
 * and conditions contained in a signed written agreement between
 * you and ecsec GmbH.
 *
 ***************************************************************************/

package com.epotheke.sdk

import WebsocketCommon
import WebsocketListenerCommon
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import io.github.oshai.kotlinlogging.KotlinLogging
import org.openecard.android.activation.AndroidContextManager
import org.openecard.android.activation.OpeneCard
import org.openecard.android.utils.NfcIntentHelper
import org.openecard.mobile.activation.*

private val logger = KotlinLogging.logger {}

class SdkCore(
    private val ctx: Activity,
    private val cardLinkUrl: String,
    private val tenantToken: String?,
    private val cardLinkControllerCallback: CardLinkControllerCallback,
    private val cardLinkInteraction: CardLinkInteraction,
    private val sdkErrorHandler: SdkErrorHandler
) {

    private var ctxManager: AndroidContextManager? = null
    private var nfcIntentHelper: NfcIntentHelper? = null
    private var needNfc = false
    private var activation: ActivationController? = null
    private var preventAuthCallbackOnFail = false


    fun onPause() {
        nfcIntentHelper?.disableNFCDispatch();
    }

    fun onResume() {
        if (needNfc) {
            nfcIntentHelper?.enableNFCDispatch();
        }
    }

    fun initOecContext() {
        val oec = OpeneCard.createInstance()
        nfcIntentHelper = NfcIntentHelper.create(ctx)
        ctxManager = oec?.context(ctx)
        ctxManager?.initializeContext(object : StartServiceHandler {
            override fun onSuccess(actSource: ActivationSource) {
                val websocket = WebsocketCommon(cardLinkUrl, tenantToken)
                val wsListener = WebsocketListenerCommon()
                val protocols = buildProtocols(websocket, wsListener)
                activation = actSource.cardLinkFactory().create(
                    WebsocketAndroid(websocket, overridingSdkErrorHandler(sdkErrorHandler)),
                    overridingControllerCallback(protocols),
                    OverridingCardLinkInteraction(this@SdkCore, cardLinkInteraction),
                    WebsocketListenerAndroid(wsListener)
                )
            }

            override fun onFailure(ex: ServiceErrorResponse) {
                logger.error { "Failed to initialize Open eCard (code=${ex.statusCode}): ${ex.errorMessage}" }
                cleanupOecInstances()
                sdkErrorHandler.onError(ex)
            }
        })
    }

    fun destroyOecContext() {
        activation?.cancelOngoingAuthentication()
        ctxManager?.terminateContext(object : StopServiceHandler {
            override fun onSuccess() {
                // do nothing
                logger.debug { "Open eCard stopped successfully." }
                cleanupOecInstances()
            }

            override fun onFailure(ex: ServiceErrorResponse) {
                logger.error { "Failed to stop Open eCard (code=${ex.statusCode}): ${ex.errorMessage}" }
                cleanupOecInstances()
                sdkErrorHandler.onError(ex)
            }
        })
    }

    private fun cleanupOecInstances() {
        ctxManager = null
        activation = null
    }

    @SuppressLint("NewApi")
    fun onNewIntent(intent: Intent) {
        val t = intent.parcelable<Tag>(NfcAdapter.EXTRA_TAG)
        t?.let {
            needNfc = false
            ctxManager?.onNewIntent(intent)
        }
    }

    private class OverridingCardLinkInteraction(val ctx: SdkCore, val delegate: CardLinkInteraction) :
        CardLinkInteraction by delegate {
        override fun requestCardInsertion() {
            ctx.nfcIntentHelper?.enableNFCDispatch()
            ctx.needNfc = true
            delegate.requestCardInsertion()
        }
    }

    private fun overridingSdkErrorHandler(sdkErrorHandler: SdkErrorHandler): SdkErrorHandler {
       return object : SdkErrorHandler {
           override fun onError(error: ServiceErrorResponse) {
               //prevent onAuthCompletion since we don't want two callbacks if process is already failed
               preventAuthCallbackOnFail = true
               sdkErrorHandler.onError(error)
           }
       }
    }

    private fun overridingControllerCallback(protocols: Set<CardLinkProtocol>): ControllerCallback {
        return object : ControllerCallback {
            override fun onStarted() = cardLinkControllerCallback.onStarted()
            override fun onAuthenticationCompletion(p0: ActivationResult?) {
                nfcIntentHelper?.disableNFCDispatch()
                needNfc = false
                if(!preventAuthCallbackOnFail) {
                    cardLinkControllerCallback.onAuthenticationCompletion(p0, protocols)
                }
                destroyOecContext()
            }
        }
    }
}
