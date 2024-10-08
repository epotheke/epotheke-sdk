import cocoapods.open_ecard.*
import com.epotheke.sdk.CardLinkProtocol
import com.epotheke.sdk.buildProtocols
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.cinterop.ExperimentalForeignApi
import platform.darwin.NSObject

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

private val logger = KotlinLogging.logger {}

interface SdkErrorHandler {
    fun hdl(error: NSObject?)
}

@OptIn(ExperimentalForeignApi::class)
class SdkCore(
    private val cardLinkUrl: String,
    private val tenantToken: String?,
    private val cardLinkControllerCallback: CardLinkControllerCallback,
    private val cardLinkInteractionProtocol: CardLinkInteractionProtocol,
    private val sdkErrorHandler: SdkErrorHandler,
    private val nfcOpts: NFCConfigProtocol,
) {
    private var ctx: ContextManagerProtocol? = null
    private var activation: ActivationControllerProtocol? = null

    @OptIn(ExperimentalForeignApi::class)
    fun initCardLink() {
        logger.debug { cardLinkUrl }
        val oec = OpenEcardImp()
        oec.developerOptions()
        ctx = oec.context(nfcOpts as NSObject) as ContextManagerProtocol
        ctx!!.initializeContext(object : StartServiceHandlerProtocol, NSObject() {
            override fun onFailure(response: NSObject?) {
                println("Fail")
                sdkErrorHandler.hdl(response)
            }

            override fun onSuccess(source: NSObject?) {
                val src = source as ActivationSourceProtocol
                val factory = src.cardLinkFactory() as CardLinkControllerFactoryProtocol
                val ws = WebsocketCommon(cardLinkUrl, tenantToken)
                val wsListener = WebsocketListenerCommon()
                val protocols = buildProtocols(ws, wsListener)
                activation = factory.create(
                    WebsocketIos(ws),
                    withActivation = OverridingControllerCallback(protocols, cardLinkControllerCallback) as NSObject,
                    withInteraction = cardLinkInteractionProtocol as NSObject,
                    withListenerSuccessor = WebsocketListenerIos(wsListener) as NSObject,
                ) as ActivationControllerProtocol

            }


        } as NSObject)

    }

    fun terminateContext() {
        ctx!!.terminateContext(object : StopServiceHandlerProtocol, NSObject() {
            override fun onFailure(response: NSObject?) {
                logger.debug { "stopped successfully" }
            }

            override fun onSuccess() {
                logger.debug { "stopped with error" }
                sdkErrorHandler.hdl(null)
            }

        } as NSObject)

    }

    @OptIn(ExperimentalForeignApi::class)
    private class OverridingControllerCallback(
        val protocols: Set<CardLinkProtocol>,
        val cardLinkControllerCallback: CardLinkControllerCallback
    ) : ControllerCallbackProtocol, NSObject() {
        override fun onAuthenticationCompletion(result: NSObject?) {
            cardLinkControllerCallback.onAuthenticationCompletion(result as ActivationResultProtocol, protocols)
        }

        override fun onStarted() {
            cardLinkControllerCallback.onStarted()
        }

    }

}

