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
import org.openecard.android.activation.AndroidContextManager
import org.openecard.android.activation.OpeneCard
import org.openecard.android.utils.NfcIntentHelper
import org.openecard.mobile.activation.*

private val logger = KotlinLogging.logger {}

class Epotheke(
    private val ctx: Activity,
    private val cardlinkUrl: String,
    private val cardlinkControllerCallback: CardlinkControllerCallback,
    private val cardLinkInteraction: CardLinkInteraction
) {

    var oec: OpeneCard? = null
    var ctxManager: AndroidContextManager? = null
    var activationSource: ActivationSource? = null
    var nfcIntentHelper: NfcIntentHelper? = null
    var needNfc = false


    fun onPause() {
        nfcIntentHelper?.disableNFCDispatch();
    }

    fun onResume() {
        if (needNfc) {
            nfcIntentHelper?.enableNFCDispatch();
        }
    }

    fun initOecContext() {
        oec = OpeneCard.createInstance()
        nfcIntentHelper = NfcIntentHelper.create(ctx)
        ctxManager = oec?.context(ctx)
        ctxManager?.initializeContext(object : StartServiceHandler {
            override fun onSuccess(actSource: ActivationSource) {
                activationSource = actSource
                val websocket = WebsocketAndroid(cardlinkUrl)
                val wsListener = WebsocketListener()
                val protocols = buildProtocols(websocket, wsListener)
                actSource.cardLinkFactory().create(
                    websocket,
                    overridingControllerCallback(protocols),
                    OverridingCardlinkInteraction(this@Epotheke, cardLinkInteraction),
                    wsListener
                )
            }

            override fun onFailure(ex: ServiceErrorResponse) {
                logger.error { "Failed to initialize Open eCard (code=${ex.statusCode}): ${ex.errorMessage}" }
                cleanupOecInstances()
            }
        })
    }

    fun destroyOecContext() {
        ctxManager?.terminateContext(object : StopServiceHandler {
            override fun onSuccess() {
                // do nothing
                logger.debug { "Open eCard stopped successfully." }
                cleanupOecInstances()
            }

            override fun onFailure(ex: ServiceErrorResponse) {
                logger.error { "Failed to stop Open eCard (code=${ex.statusCode}): ${ex.errorMessage}" }
                cleanupOecInstances()
            }
        })
    }

    protected fun cleanupOecInstances() {
        oec = null
        ctxManager = null
        activationSource = null
    }

    private fun buildProtocols(websocket: WebsocketAndroid, wsListener: WebsocketListener): Set<CardLinkProtocol> {
        return setOf(
            ErezeptProtocolImp(websocket)
        ).onEach { p ->
            p.registerListener(wsListener);
        }
    }

    @SuppressLint("NewApi")
    fun onNewIntent(intent: Intent) {
        val t = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        t?.let {
            ctxManager?.onNewIntent(intent)
        }
    }

    private class OverridingCardlinkInteraction(val ctx: Epotheke, val delegate: CardLinkInteraction): CardLinkInteraction by delegate {
        override fun requestCardInsertion() {
            ctx.nfcIntentHelper?.enableNFCDispatch()
            ctx.needNfc = true
            delegate.requestCardInsertion()
        }
        override fun onCardInteractionComplete() {
            ctx.nfcIntentHelper?.disableNFCDispatch()
            ctx.needNfc = false
            delegate.onCardInteractionComplete()
        }
    }

    private fun overridingControllerCallback(protocols: Set<CardLinkProtocol>): ControllerCallback {
        return object : ControllerCallback {
            override fun onStarted() = cardlinkControllerCallback.onStarted()
            override fun onAuthenticationCompletion(p0: ActivationResult?) {
                nfcIntentHelper?.disableNFCDispatch()
                needNfc = false
                cardlinkControllerCallback.onAuthenticationCompletion(p0, protocols)
            }
        }
    }
}
