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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel

private val logger = KotlinLogging.logger { }

interface CardLinkProtocol {
    fun messageHandler(msg: String): (suspend () -> Unit)?
}

internal fun <T> protocolChannel() =
    Channel<T>(
        1,
        BufferOverflow.DROP_OLDEST,
    ) { logger.warn { "InputChannel dropped message: $it" } }

abstract class CardLinkProtocolBase(
    ws: WebsocketCommon,
) : CardLinkProtocol {
    init {
        ws.addProtocol(this)
    }
}
