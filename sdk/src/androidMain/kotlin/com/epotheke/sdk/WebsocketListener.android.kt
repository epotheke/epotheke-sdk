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

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.openecard.mobile.activation.Websocket

private val logger = KotlinLogging.logger {}

class WebsocketListener() : ChannelDispatcher,
    org.openecard.mobile.activation.WebsocketListener {

    private val channels: MutableList<Channel<String>> = ArrayList<Channel<String>>()

    override fun onOpen(p0: Websocket) {
    }

    override fun onClose(p0: Websocket, p1: Int, p2: String?) {
    }

    override fun onError(p0: Websocket, p1: String) {
//        protos.map { p-> p.getErrorHandler()(p1) }
    }

    override fun onText(p0: Websocket, p1: String) {
        logger.debug { "Message from established link: $p1" }
        runBlocking {
            channels.map { c ->
                c.send(p1)
            }
        }
    }

    override fun addProtocolChannel(channel: Channel<String>) {
        channels.add(channel)
    }
}
