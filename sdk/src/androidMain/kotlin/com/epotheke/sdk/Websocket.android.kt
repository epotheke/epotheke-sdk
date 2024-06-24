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
import WiredWSListener
import android.util.Log
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import org.openecard.mobile.activation.Websocket
import org.openecard.mobile.activation.WebsocketException
import org.openecard.mobile.activation.WebsocketListener

private val logger = KotlinLogging.logger {}

private class WiredWSListenerImplementation(
    private val ws: WebsocketAndroid,
    private val wsListener: WebsocketListener,
) : WiredWSListener {
    override fun onOpen() = wsListener.onOpen(ws)
    override fun onClose(code: Int, reason: String?) = wsListener.onClose(ws, code, reason)
    override fun onError(error: String) = wsListener.onError(ws, error)
    override fun onText(msg: String) = wsListener.onText(ws, msg)
}


class WebsocketAndroid(
    url: String,
) : Websocket {

    private val commonWS = WebsocketCommon(url)

    override fun setListener(wsListener: WebsocketListener) =
        commonWS.setListener(WiredWSListenerImplementation(this, wsListener))
    override fun removeListener() = commonWS.removeListener()
    override fun getUrl(): String = commonWS.getUrl()
    override fun setUrl(url: String) = commonWS.setUrl(url)

    override fun getSubProtocol(): String? = commonWS.getSubProtocol()
    override fun isOpen(): Boolean = commonWS.isOpen()
    override fun isFailed(): Boolean = commonWS.isFailed()

    @Throws(WebsocketException::class)
    override fun connect() {
        try {
            commonWS.connect()
        } catch (e: Exception) {
            throw WebsocketException(e.message ?: "Unknown error", e)
        }
    }

    @Throws(WebsocketException::class)
    override fun close(statusCode: Int, reason: String?) {
        try {
            commonWS.close(statusCode, reason)
        } catch (e: Exception) {
            throw WebsocketException(e.message ?: "Unknown error", e)
        }
    }

    @Throws(WebsocketException::class)
    override fun send(data: String) {
        logger.debug { "sending via socket: $data" }
        try {
            commonWS.send(data)
        } catch (e: Exception) {
            throw WebsocketException(e.message ?: "Unknown error", e)
        }
    }
}

