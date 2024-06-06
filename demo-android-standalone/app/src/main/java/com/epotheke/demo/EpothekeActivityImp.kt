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

package com.epotheke.demo

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.epotheke.sdk.CardLinkProtocol
import com.epotheke.sdk.EpothekeActivity
import io.github.oshai.kotlinlogging.KotlinLogging
import org.openecard.mobile.activation.*
import java.util.*


private val LOG = KotlinLogging.logger {}

/**
 * The address of the service to connect to is set to a mock service for this demo app.
 */
private const val mockServiceUrl = "https://epotheke.mock.ecsec.services/cardlink"


/**
 * To use the Epotheke SDK within an app, the easiest way is to extend the abstract EpothekeActivity
 * provided by the SDK.
 * The EpothekeActivity will instantiate the framework and start a cardlink connection process in its onCreate() method.
 * The controlflow during the connection establishment via Cardlink is encapsulated within the SDK.
 * The interaction with the app and the user is handled via CallbackHandler interfaces, which have to be
 * implemented within the app and handed to the SDK via the implementation of abstract getter methods
 * of the EpothekeActivity.
 *
 * EpothekeActivity will also terminate and cleanup the SDK if the Activity gets destroyed.
 * In also handles the switching of androids nfc foreground dispatch for nfc usage in onPause and onResume events.
 * @author Florian Otto
 */
class EpothekeActivityImp : EpothekeActivity() {
    /**
     * This method has to return the url of the epotheke service, and will be called in
     * EpothekeActivity during startup.
     *
     * @return
     */
    override fun getCardlinkUrl(): String {
        return mockServiceUrl + "?token=" + UUID.randomUUID()
    }

    /**
     * This method has to return an implementation of the CardLinkInteraction interface,
     * which contains handlers for different steps in the cardlink process.
     *
     * @return CardLinkInteraction
     */
    override fun getCardLinkInteraction(): CardLinkInteraction {
        return CardLinkInteractionImp()
    }

    /**
     * This method has to return the ControllerCallback which contains events for the start and the
     * end of the link establishment
     *
     * @return ControllerCallback
     */
    override fun getControllerCallback(): ControllerCallback {
        return ControllerCallbackImp()
    }

    /**
     * This method has to return protocols which contain implementations for processes after
     * a link is established.
     *
     * This is currently not used.
     *
     * @return Set<CardLinkProtocol>
    </CardLinkProtocol> */
    override fun getProtocols(): Set<CardLinkProtocol> {
        return HashSet()
    }

    /**
     * Implementation of the CardLinkInteraction
     * The different methods get called during the interaction with the card and may require
     * the user to interact with the device, for example to enter a TAN etc.
     */
    private inner class CardLinkInteractionImp : CardLinkInteraction {
        /**
         * Called when the SDK needs to communicates with the card.
         * The user must read the CAN from it card and enter it within the app.
         * The given CAN has then be handed back to the SDK via
         * confirmPasswordOperation.confirmPassowrd("<CANVALUE>")
         *
         * @param confirmPasswordOperation
        </CANVALUE> */
        override fun onCanRequest(confirmPasswordOperation: ConfirmPasswordOperation) {
            LOG.debug { "EpothekeImplementation onCanRequest" }
            getValueFromUser("Please provide CAN of card", "000000") { value ->
                confirmPasswordOperation.confirmPassword(value)
            }
        }


        /**
         * Called during the cardlink establishment.
         * The service needs the cellphone number of the users device to send an sms containing a TAN for verification.
         * The app should ask the user to enter a number, which is then handed back to the SDK via
         * confirmTextOperation.confirmText("<NUMBER>");
         *
         * Note: The mock service won't send real sms to the given number but also will accept any TAN.
         * The number must however be in a format valid for german mobile numbers to be accepted by the mock service.
         * @param confirmTextOperation
        </NUMBER> */
        override fun onPhoneNumberRequest(confirmTextOperation: ConfirmTextOperation) {
            LOG.debug { "EpothekeImplementation onPhoneNumberRequest" }
            getValueFromUser(
                "Please provide valid german phone number (mock won't send sms)",
                "+4915123456789"
            ) { value ->
                confirmTextOperation.confirmText(value)
            }
        }

        /**
         * Called during the cardlink establishment.
         * The user will get an SMS containing a TAN for verification purposes.
         * The given TAN has then be handed back to the SDK via
         * confirmPasswordOperation.confirmPassowrd("<TANVALUE>")
         *
         * Note: The mock service will accept any TAN.
         * @param confirmPasswordOperation
        </TANVALUE> */
        override fun onSmsCodeRequest(confirmPasswordOperation: ConfirmPasswordOperation) {
            LOG.debug { "EpothekeImplementation onSmsCodeRequest" }
            getValueFromUser(
                "Please provide TAN from sms (mock accepts all)",
                "123456"
            ) { value ->
                confirmPasswordOperation.confirmPassword(value)
            }
        }

        /**
         * Called during the connection establishment, when the SDK has to communicate with the card.
         * The method is called to enable the app to inform the user, to bring the card to
         * the devices nfc sensor.
         */
        override fun requestCardInsertion() {
            LOG.debug { "EpothekeImplementation requestCardInsertion" }
            runOnUiThread {
                setBusy(false)
                showInfo("Please provide card")
            }
        }

        /**
         * This is not called in android and used only in iOS.
         *
         * @param nfcOverlayMessageHandler
         */
        override fun requestCardInsertion(nfcOverlayMessageHandler: NFCOverlayMessageHandler) {
            LOG.debug { "EpothekeImplementation requestCardInsertion with nfcOverlayMessageHandler" }
        }

        /**
         * Gets called, when the communication with the card is over.
         * The app may inform the user that the card is no longer needed.
         */
        override fun onCardInteractionComplete() {
            LOG.debug { "EpothekeImplementation onCardInteractionComplete" }
        }

        /**
         * Gets called, when the intent from android of a detected card, is consumed by the SDK.
         * The app may inform the user, that the card was detected.
         */
        override fun onCardRecognized() {
            LOG.debug { "EpothekeImplementation onCardRecognized" }
            runOnUiThread {
                setBusy(true)
                showInfo("Found card - please don't move card and device")
            }
        }

        /**
         * Gets called, when the card is removed from the reader.
         * The app may inform the user, that the app was removed.
         */
        override fun onCardRemoved() {
            LOG.debug { "EpothekeImplementation onCardRemoved" }
        }
    }


    /**
     * The ControllerCallback informs about the start of the CardLink process and returns its results.
     */
    private inner class ControllerCallbackImp : ControllerCallback {
        /**
         * Called when the process starts.
         * The app may inform the user.
         */
        override fun onStarted() {
            LOG.debug { "EpothekeImplementation onStarted" }
            showInfo("Process started...")
        }

        /**
         * Called when the connection establishment finishes.
         * The Result contains values for further usage in Protocols when the connection was
         * successfully established.
         * If something went wrong the Result will contain an error.
         */
        override fun onAuthenticationCompletion(activationResult: ActivationResult) {
            LOG.debug { "EpothekeImplementation onAuthenticationCompletion" }
            LOG.debug { (activationResult.toString()) }
            runOnUiThread {
                setBusy(false)
                val sb = StringBuilder()
                sb.append(if (activationResult.resultCode == ActivationResultCode.OK) "SUCCESS" else "FAIL")
                sb.append("\n")
                if (activationResult.resultCode == ActivationResultCode.OK) {
                    for (key in activationResult.resultParameterKeys) {
                        sb.append(key)
                        sb.append(": ")
                        sb.append(activationResult.resultParameterKeys)
                        sb.append("\n")
                    }
                } else {
                    sb.append(activationResult.errorMessage)
                }
                showInfo(sb.toString())
                val btn_cancel = findViewById<Button>(R.id.btn_cancel)
                btn_cancel.text = "FINISH"
            }
        }
    }

    /**
     * Functional interface to be able to bind sdk-callbacks to button taps
     */
    private fun interface ButtonAction {
        fun act(value: String)
    }

    /**
     * Updates the info label to inform the user.
     *
     * @param text
     */
    fun showInfo(text: String?) {
        runOnUiThread {
            val t = findViewById<TextView>(R.id.statusText)
            t.text = text
        }
    }

    /**
     * Shows inputfield and button to let the user enter data.
     *
     * @param infoText
     * @param defaultValue
     * @param action
     */
    private fun getValueFromUser(infoText: String, defaultValue: String, action: ButtonAction) {
        runOnUiThread {
            showInfo(infoText)
            val inputField = findViewById<TextView>(R.id.input)
            inputField.text = defaultValue
            val btn_ok = findViewById<Button>(R.id.btn_ok)
            btn_ok.setOnClickListener { _: View? ->
                setInputActive(false)
                val input = findViewById<TextView>(R.id.input)
                action.act(input.text.toString())
            }
            setInputActive(true)
        }
    }

    /**
     * Shows and hides input elements
     *
     * @param active
     */
    private fun setInputActive(active: Boolean) {
        runOnUiThread {
            val inputField = findViewById<TextView>(R.id.input)
            inputField.visibility = if (active) View.VISIBLE else View.INVISIBLE
            val btn_ok = findViewById<Button>(R.id.btn_ok)
            btn_ok.visibility = if (active) View.VISIBLE else View.INVISIBLE
            setBusy(!active)
        }
    }

    /**
     * Shows and hides busy indicator.
     *
     * @param busy
     */
    private fun setBusy(busy: Boolean) {
        val p = findViewById<ProgressBar>(R.id.busy)
        p.visibility = if (busy) View.VISIBLE else View.INVISIBLE
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.epo_layout)
        val serviceLabel = findViewById<TextView>(R.id.service)
        serviceLabel.text = """
            ${serviceLabel.text}
            $mockServiceUrl
            """.trimIndent()
        val inputField = findViewById<TextView>(R.id.input)
        inputField.isEnabled = true
        setInputActive(false)
        val cancelButton = findViewById<Button>(R.id.btn_cancel)
        cancelButton.setOnClickListener { _: View? -> finish() }
        super.onCreate(savedInstanceState)
    }

}
