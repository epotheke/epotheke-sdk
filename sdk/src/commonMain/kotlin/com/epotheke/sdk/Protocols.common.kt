package com.epotheke.sdk

import com.epotheke.erezept.model.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.cancellation.CancellationException

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


class PrescriptionProtocolException(val msg: GenericErrorMessage) : Exception()

interface ChannelDispatcher {
    fun addProtocolChannel(channel: Channel<String>)
}

interface CardLinkProtocol

interface PrescriptionProtocol : CardLinkProtocol {
    @Throws(PrescriptionProtocolException::class, CancellationException::class)
    suspend fun requestPrescriptions(req: RequestPrescriptionList): AvailablePrescriptionLists

    @Throws(PrescriptionProtocolException::class, CancellationException::class)
    suspend fun selectPrescriptions(selection: SelectedPrescriptionList): SelectedPrescriptionListResponse
}
