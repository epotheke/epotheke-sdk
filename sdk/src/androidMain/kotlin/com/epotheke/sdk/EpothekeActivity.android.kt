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
import android.os.Bundle
import io.github.oshai.kotlinlogging.KotlinLogging
import org.openecard.mobile.activation.CardLinkInteraction

private val logger = KotlinLogging.logger {}

abstract class EpothekeActivity : Activity() {

    var epotheke: Epotheke? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        epotheke = Epotheke(
            this,
            getCardLinkUrl(),
            getControllerCallback(),
            getCardLinkInteraction(),
            getSdkErrorHandler(),
        ).apply {
            initOecContext()
        }
    }

    override fun onPause() {
        epotheke?.onPause()
        super.onPause()
    }

    override fun onResume() {
        epotheke?.onResume()
        super.onResume()
    }

    override fun onDestroy() {
        epotheke?.destroyOecContext()
        super.onDestroy()
    }

    @SuppressLint("NewApi")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val t = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        t?.let {
            epotheke?.onNewIntent(intent)
        }
    }

    abstract fun getCardLinkUrl(): String
    abstract fun getControllerCallback(): CardLinkControllerCallback
    abstract fun getCardLinkInteraction(): CardLinkInteraction
    abstract fun getSdkErrorHandler(): SdkErrorHandler

}
