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

private val logger = KotlinLogging.logger { }

interface CardLinkProtocol {
    fun filterMessage(msg: String): Boolean
}

class FilteringChannel(
    val protocol: CardLinkProtocol,
    val inputChannel: Channel<String> = Channel(10),
) : Channel<String> by inputChannel {
    /**
     * Put msg to channel if it passes filter
     */
    override suspend fun send(element: String) {
        if (protocol.filterMessage(element)) {
            inputChannel.send(element)
        }
    }
}

interface ChannelDispatcher {
    fun addProtocolChannel(channel: FilteringChannel)
}

abstract class CardLinkProtocolBase(
    ws: WebsocketCommon,
) : CardLinkProtocol {
    protected val inputChannel = FilteringChannel(this)

    init {
        ws.addProtocolChannel(inputChannel)
    }
}
