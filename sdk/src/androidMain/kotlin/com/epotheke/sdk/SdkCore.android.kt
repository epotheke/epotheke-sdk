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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized
import org.openecard.android.activation.AndroidContextManager
import org.openecard.android.activation.OpeneCard
import org.openecard.android.utils.NfcIntentHelper
import org.openecard.mobile.activation.*

private val logger = KotlinLogging.logger {}

class SdkCore(
    private val ctx: Activity,
    private val cardLinkControllerCallback: CardLinkControllerCallback,
    private val cardLinkInteraction: CardLinkInteraction,
    private val sdkErrorHandler: SdkErrorHandler
) {

    private var ctxManager: AndroidContextManager? = null
    private var nfcIntentHelper: NfcIntentHelper? = null
    private var needNfc = false
    private var currentActivation: ActivationController? = null

    private var activationSource: ActivationSource? = null

    fun onPause() {
        nfcIntentHelper?.disableNFCDispatch();
    }

    fun onResume() {
        if (needNfc) {
            nfcIntentHelper?.enableNFCDispatch();
        }
    }

    private val sdkLock = Object()
    @Volatile
    private var currentActivationSession: Any? = null
    @Volatile
    private var waitingActivations = 0

    fun activationsActive(): Boolean {
        return currentActivationSession != null || waitingActivations > 0
    }
    /**
     * if there is an activation ongoing:
     * if wait for slot is true activation will start when previous activation finishes
     * else will return immediately
     *
     * if none is ongoing it starts an activation
     *
     */
    @OptIn(InternalCoroutinesApi::class)
    fun activate(waitForSlot: Boolean, cardLinkUrl: String, tenantToken: String?) {
        synchronized(sdkLock) {
            waitingActivations = waitingActivations.inc()
            while (currentActivationSession != null) {
                if(waitForSlot) {
                    sdkLock.wait()
                }else {
                    waitingActivations = waitingActivations.dec()
                    return
                }
            }
            waitingActivations = waitingActivations.dec()

            val activationSession = object {}
            currentActivationSession = activationSession

            if (activationSource == null) {
                initOecContext(activationSession, cardLinkUrl, tenantToken)
            } else {
                activationSource?.let {
                    doActivation( activationSession,it, cardLinkUrl, tenantToken)
                }
            }
        }
    }

    private fun doActivation(activationSession: Any, activationSource: ActivationSource, cardLinkUrl: String, tenantToken: String?){

        val websocket = WebsocketCommon(cardLinkUrl, tenantToken)
        val wsListener = WebsocketListenerCommon()
        val protocols = buildProtocols(websocket, wsListener)
        currentActivation = activationSource.cardLinkFactory().create(
            WebsocketAndroid(websocket, overridingSdkErrorHandler(sdkErrorHandler, activationSession)),
            overridingControllerCallback(protocols, activationSession),
            OverridingCardLinkInteraction(activationSession,this@SdkCore, cardLinkInteraction),
            WebsocketListenerAndroid(wsListener)
        )
    }

    fun cancelOngoingActivation(){
        currentActivation?.cancelOngoingAuthentication()
    }

    @OptIn(InternalCoroutinesApi::class)
    private fun initOecContext(activationSession: Any, cardLinkUrl: String, tenantToken: String?) {

        val oec = OpeneCard.createInstance()
        nfcIntentHelper = NfcIntentHelper.create(ctx)
        ctxManager = oec.context(ctx)

        ctxManager?.initializeContext(object : StartServiceHandler {
            override fun onSuccess(actSource: ActivationSource) {
                activationSource = actSource
                doActivation(activationSession,actSource,  cardLinkUrl, tenantToken)
            }

            override fun onFailure(ex: ServiceErrorResponse) {
                logger.error { "Failed to initialize Open eCard (code=${ex.statusCode}): ${ex.errorMessage}" }

                synchronized(sdkLock) {
                   if(activationSession == currentActivationSession){
                       nfcIntentHelper = null
                       ctxManager = null
                       currentActivationSession = null
                       sdkErrorHandler.onError(ex)
                       sdkLock.notify()
                   }
                }
            }
        })
    }

    @OptIn(InternalCoroutinesApi::class)
    fun destroyOecContext() {
        logger.debug { "SdkCore.android - destroying oecContext." }

        synchronized(sdkLock){
            //cancelling here leads to a authcompletion with cancel
            //the handler however is synced so it will be called after this block here
            //and its session guard will prevent to call the app
            cancelOngoingActivation()
            val ctxManagerToBeDestroyed = ctxManager ?: return

            nfcIntentHelper = null
            activationSource = null
            ctxManager = null

            ctxManagerToBeDestroyed.terminateContext(object : StopServiceHandler {
                override fun onSuccess() {
                    logger.debug { "Open eCard stopped successfully." }
                }

                override fun onFailure(ex: ServiceErrorResponse) {
                    logger.error { "Failed to stop Open eCard (code=${ex.statusCode}): ${ex.errorMessage}" }
                }
            })
            sdkLock.notify()
        }
    }

    @SuppressLint("NewApi")
    fun onNewIntent(intent: Intent) {
        val t = intent.parcelable<Tag>(NfcAdapter.EXTRA_TAG)
        t?.let {
            needNfc = false
            ctxManager?.onNewIntent(intent)
        }
    }

    private class OverridingCardLinkInteraction(val activationSession: Any, val ctx: SdkCore, val delegate: CardLinkInteraction) :
        CardLinkInteraction {
        override fun requestCardInsertion() {
            if (activationSession == ctx.currentActivationSession) {
                ctx.nfcIntentHelper?.enableNFCDispatch()
                ctx.needNfc = true
                delegate.requestCardInsertion()
            }
        }

        override fun requestCardInsertion(p0: NFCOverlayMessageHandler?) {
            //ont used in android
            //if (activationSession == ctx.currentActivationSession) delegate.requestCardInsertion()
        }

        override fun onCardInteractionComplete() {
            if (activationSession == ctx.currentActivationSession) delegate.onCardInteractionComplete()
        }

        override fun onCardInserted() {
            if (activationSession == ctx.currentActivationSession) delegate.onCardInserted()
        }

        override fun onCardInsufficient() {
            if (activationSession == ctx.currentActivationSession) delegate.onCardInsufficient()
        }

        override fun onCardRecognized() {
            if (activationSession == ctx.currentActivationSession) delegate.onCardRecognized()
        }

        override fun onCardRemoved() {
            if (activationSession == ctx.currentActivationSession) delegate.onCardRemoved()
        }

        override fun onCanRequest(p0: ConfirmPasswordOperation?) {
            if (activationSession == ctx.currentActivationSession) delegate.onCanRequest(p0)
        }

        override fun onCanRetry(p0: ConfirmPasswordOperation?, p1: String?, p2: String?) {
            if (activationSession == ctx.currentActivationSession) delegate.onCanRetry(p0,p1,p2)
        }

        override fun onPhoneNumberRequest(p0: ConfirmTextOperation?) {
            if (activationSession == ctx.currentActivationSession) delegate.onPhoneNumberRequest(p0)
        }

        override fun onPhoneNumberRetry(p0: ConfirmTextOperation?, p1: String?, p2: String?) {
            if (activationSession == ctx.currentActivationSession) delegate.onPhoneNumberRetry(p0,p1,p2)
        }

        override fun onSmsCodeRequest(p0: ConfirmPasswordOperation?) {
            if (activationSession == ctx.currentActivationSession) delegate.onSmsCodeRequest(p0)
        }

        override fun onSmsCodeRetry(p0: ConfirmPasswordOperation?, p1: String?, p2: String?) {
            if (activationSession == ctx.currentActivationSession) delegate.onSmsCodeRetry(p0,p1,p2)
        }
    }

    @OptIn(InternalCoroutinesApi::class)
    private fun overridingSdkErrorHandler(sdkErrorHandler: SdkErrorHandler, activationSession: Any): SdkErrorHandler {
        return object : SdkErrorHandler {
            override fun onError(error: ServiceErrorResponse) {
                if(activationSession == currentActivationSession) {
                    synchronized(sdkLock){
                        //prevent onAuthCompletion since we don't want two callbacks if process is already failed
                        cancelOngoingActivation()
                        currentActivation = null
                        currentActivationSession = null
                        sdkErrorHandler.onError(error)
                        sdkLock.notify()
                    }
                } else {
                    logger.warn { "SdkError handler called from invalid/old activation session $activationSession (current is $currentActivationSession). Don't notify current handler." }
                }
            }
        }
    }

    @OptIn(InternalCoroutinesApi::class)
    private fun overridingControllerCallback(protocols: Set<CardLinkProtocol>, activationSession: Any): ControllerCallback {
        return object : ControllerCallback {

            override fun onStarted() {
                if(activationSession == currentActivationSession) {
                    cardLinkControllerCallback.onStarted()
                } else {
                    logger.warn { "Controllercallback onStarted-handler called from invalid/old activation session $activationSession (current is $currentActivationSession). Don't notify current handler." }
                }
            }
            override fun onAuthenticationCompletion(p0: ActivationResult?) {
                synchronized(sdkLock) {
                    if (activationSession == currentActivationSession) {
                        if (!ctx.isDestroyed) {
                            nfcIntentHelper?.disableNFCDispatch()
                        }

                        cardLinkControllerCallback.onAuthenticationCompletion(
                            p0?.adjustedResults(),
                           protocols
                        )

                        needNfc = false
                        currentActivation = null
                        currentActivationSession = null
                        sdkLock.notify()

                    } else {
                        logger.warn { "Controllercallback onAuthenticationCompletion-handler called from invalid/old activation session $activationSession (current is $currentActivationSession). Don't notify current handler." }
                    }

                }

            }
        }
    }

    fun ActivationResult.adjustedResults(): ActivationResult {
        val origResult = this
        return object : ActivationResult by origResult {
            override fun getResultParameter(key: String): String? {
                return if (key == "CardLink::PERSONAL_DATA") {
                    origResult.getResultParameter(key)?.let {
                        decodeHexPersonalDataXml(it)
                    }?.json()
                } else {
                    origResult.getResultParameter(key)
                }
            }
        } as ActivationResult
    }
}
