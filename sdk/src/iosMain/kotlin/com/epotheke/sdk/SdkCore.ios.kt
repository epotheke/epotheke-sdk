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

import cocoapods.open_ecard.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCondition
import platform.darwin.*

private val logger = KotlinLogging.logger {}

interface SdkErrorHandler {
    fun hdl(code: String, error: String)
}

@OptIn(ExperimentalForeignApi::class)
class SdkCore(
    private val cardLinkControllerCallback: CardLinkControllerCallback,
    private val cardLinkInteractionProtocol: CardLinkInteractionProtocol,
    private val sdkErrorHandler: SdkErrorHandler,
    private val nfcOpts: NFCConfigProtocol,
) {
    private var ctxManager: ContextManagerProtocol? = null
    private var currentActivation: ActivationControllerProtocol? = null
    private var activationSource: ActivationSourceProtocol? = null
    private var dbgLogLevel = false
    private var logMessageHandler: LogMessageHandlerProtocol? = null

    fun setDebugLogLevel() {
       dbgLogLevel = true
    }
    fun setLogMessageHandler(handler: LogMessageHandlerProtocol){
       logMessageHandler = handler
    }

    private val sdkLock = NSCondition()
    private var currentActivationSession: Any? = null
    private var waitingActivations = 0

    fun activationsActive(): Boolean {
        return currentActivationSession != null || waitingActivations > 0
    }

    fun activate(waitForSlot: Boolean, cardLinkUrl: String, tenantToken: String?) {
        sdkLock.lock()
        waitingActivations = waitingActivations.inc()
        while(currentActivationSession != null){
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

        if(activationSource == null){
            initOecContext(activationSession, cardLinkUrl, tenantToken)
        }else {
            activationSource?.let {
                doActivation( activationSession,it, cardLinkUrl, tenantToken)
            }
        }
        sdkLock.unlock()
    }



    private fun doActivation(activationSession: Any, activationSource: ActivationSourceProtocol,  cardLinkUrl: String, tenantToken: String?){
        val ws = WebsocketCommon(cardLinkUrl, tenantToken)
        val wsListener = WebsocketListenerCommon()
        val protocols = buildProtocols(ws, wsListener)
        val factory = activationSource.cardLinkFactory() as CardLinkControllerFactoryProtocol
        currentActivation = factory.create(
            WebsocketIos(ws, overridingSdkErrorHandler(sdkErrorHandler, activationSession)),
            withActivation = OverridingControllerCallback(this@SdkCore, protocols, cardLinkControllerCallback, activationSession) as NSObject,
            withInteraction = OverridingCardLinkInteraction(activationSession, this@SdkCore, cardLinkInteractionProtocol),
            withListenerSuccessor = WebsocketListenerIos(wsListener) as NSObject,
        ) as ActivationControllerProtocol
    }

    fun cancelOngoingActivation() {
        dispatch_async(dispatch_get_main_queue()) {
            currentActivation?.cancelOngoingAuthentication()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun initOecContext(activationSession: Any, cardLinkUrl: String, tenantToken: String?) {
        val oec = OpenEcardImp()

        if(dbgLogLevel){
            val devOpts = oec.developerOptions()
            try{
                (devOpts as DeveloperOptionsProtocol).setDebugLogLevel()
                if(logMessageHandler != null) {
                    (devOpts as DeveloperOptionsProtocol).registerLogHandler(logMessageHandler as NSObject)
                }
            } catch (e: Exception){
                logger.warn { "Could not set loglevel to DEBUG" }
            }
        }
        ctxManager = oec.context(nfcOpts as NSObject) as ContextManagerProtocol
        ctxManager?.initializeContext(object : StartServiceHandlerProtocol, NSObject() {

            override fun onSuccess(source: NSObject?) {
                (source as? ActivationSourceProtocol)?.let {
                    activationSource = source
                    doActivation(activationSession, source, cardLinkUrl, tenantToken)
                }
            }

            override fun onFailure(response: NSObject?) {

                val serviceError = response as ServiceErrorResponseProtocol

                logger.error { "Failed to initialize Open eCard (code=${serviceError.getStatusCode()}): ${serviceError.getErrorMessage()}" }
                sdkLock.lock()
                if(activationSession == currentActivationSession){
                    ctxManager = null
                    currentActivationSession = null
                    sdkErrorHandler.hdl(
                        serviceError.getStatusCode().name,
                        serviceError.getErrorMessage() ?: "no message"
                    )
                    sdkLock.signal()
                }
                sdkLock.unlock()
            }

        } as NSObject)

    }


    fun destroyOecContext() {
        logger.debug { "SdkCore - destroying oecContext." }
        sdkLock.lock()

        cancelOngoingActivation()
        val ctxManagerToBeDestroyed = ctxManager ?: return
        activationSource = null
        ctxManager = null

        ctxManagerToBeDestroyed.terminateContext(object : StopServiceHandlerProtocol, NSObject() {
            override fun onSuccess() {
                logger.debug { "Cardlink sdk stopped successfully" }
            }

            override fun onFailure(response: NSObject?) {
                logger.warn { "Cardlink sdk stopped with error: ${(response as ServiceErrorResponseProtocol).getErrorMessage()}" }
            }
        } as NSObject)
        sdkLock.signal()
        sdkLock.unlock()
    }
    private class OverridingCardLinkInteraction(val activationSession: Any, val ctx: SdkCore, val delegate: CardLinkInteractionProtocol) :
        NSObject(), CardLinkInteractionProtocol {
        override fun requestCardInsertion() {
            if (activationSession == ctx.currentActivationSession) {
                delegate.requestCardInsertion()
            }
        }

        override fun requestCardInsertion(msgHandler: NSObject?) {
            if (activationSession == ctx.currentActivationSession) {
                delegate.requestCardInsertion(msgHandler)
            }
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

        override fun onCanRequest(enterCan: NSObject?) {
            if (activationSession == ctx.currentActivationSession) delegate.onCanRequest(enterCan)
        }

        override fun onCanRetry(enterCan: NSObject?, withResultCode: String?, withErrorMessage: String?) {
            if (activationSession == ctx.currentActivationSession) delegate.onCanRetry(enterCan,withResultCode,withErrorMessage)
        }

        override fun onPhoneNumberRequest(enterPhoneNumber: NSObject?) {
            if (activationSession == ctx.currentActivationSession) delegate.onPhoneNumberRequest(enterPhoneNumber)
        }

        override fun onPhoneNumberRetry(enterPhoneNumber: NSObject?, withResultCode: String?, withErrorMessage: String?) {
            if (activationSession == ctx.currentActivationSession) delegate.onPhoneNumberRetry(enterPhoneNumber,withResultCode,withErrorMessage)
        }

        override fun onSmsCodeRequest(smsCode: NSObject?) {
            if (activationSession == ctx.currentActivationSession) delegate.onSmsCodeRequest(smsCode)
        }

        override fun onSmsCodeRetry(smsCode: NSObject?, withResultCode: String?, withErrorMessage: String?) {
            if (activationSession == ctx.currentActivationSession) delegate.onSmsCodeRetry(smsCode,withResultCode,withErrorMessage)
        }
    }

    private fun overridingSdkErrorHandler(sdkErrorHandler: SdkErrorHandler, activationSession: Any): SdkErrorHandler{
        return object : SdkErrorHandler {
            override fun hdl(code: String, error: String) {
                if(activationSession == currentActivationSession){
                    sdkLock.lock()
                    cancelOngoingActivation()
                    currentActivation = null
                    currentActivationSession = null
                    sdkErrorHandler.hdl(code, error)
                    sdkLock.signal()
                    sdkLock.unlock()
                } else {
                    logger.warn { "SdkError handler called from invalid/old activation session $activationSession (current is $currentActivationSession). Don't notify current handler." }
                }
            }
        }
    }


    @OptIn(ExperimentalForeignApi::class)
    private class OverridingControllerCallback(
        val sdk: SdkCore,
        val protocols: Set<CardLinkProtocol>,
        val cardLinkControllerCallback: CardLinkControllerCallback,
        val activationSession: Any
    ) : ControllerCallbackProtocol, NSObject() {

        override fun onStarted() {
            if(activationSession == sdk.currentActivationSession) {
                cardLinkControllerCallback.onStarted()
            } else {
                logger.warn { "Controllercallback onStarted-handler called from invalid/old activation session $activationSession (current is $sdk.currentActivationSession). Don't notify current handler." }
            }
        }

        override fun onAuthenticationCompletion(result: NSObject?) {
            sdk.sdkLock.lock()
            if(activationSession == sdk.currentActivationSession){
                cardLinkControllerCallback.onAuthenticationCompletion(result as ActivationResultProtocol, protocols)
                sdk.currentActivation = null
                sdk.currentActivationSession = null
                sdk.sdkLock.signal()
            } else {
                logger.warn { "Controllercallback onAuthenticationCompletion-handler called from invalid/old activation session $activationSession (current is $sdk.currentActivationSession). Don't notify current handler." }
            }
            sdk.sdkLock.unlock()
        }
    }

}

