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
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.epotheke.erezept.model.AvailablePrescriptionLists
import com.epotheke.erezept.model.MedicationCompounding
import com.epotheke.erezept.model.MedicationFreeText
import com.epotheke.erezept.model.MedicationIngredient
import com.epotheke.erezept.model.MedicationPzn
import com.epotheke.erezept.model.PrescriptionBundle
import com.epotheke.erezept.model.RequestPrescriptionList
import com.epotheke.erezept.model.SelectedPrescriptionList
import com.epotheke.erezept.model.SupplyOptionsType
import com.epotheke.sdk.CardLinkProtocol
import com.epotheke.sdk.CardLinkControllerCallback
import com.epotheke.sdk.SdkActivity
import com.epotheke.sdk.PrescriptionProtocol
import com.epotheke.sdk.SdkErrorHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.openecard.mobile.activation.*
import java.util.*


private val LOG = KotlinLogging.logger {}

/**
 * The address of the service to connect to is set to a mock service for this demo app.
 */
private const val serviceUrl = "https://mock.test.epotheke.com/cardlink"


/**
 * To use the epotheke SDK within an app, the easiest way is to extend the abstract SdkActivity
 * provided by the SDK.
 * The SdkActivity will instantiate the framework and start a CardLink connection process in its onCreate() method.
 * The control flow during the connection establishment via CardLink is encapsulated within the SDK.
 * The interaction with the app and the user is handled via CallbackHandler interfaces, which have to be
 * implemented within the app and handed to the SDK via the implementation of abstract getter methods
 * of the SdkActivity.
 *
 * SdkActivity will also terminate and cleanup the SDK if the Activity gets destroyed.
 * In also handles the switching of androids nfc foreground dispatch for nfc usage in onPause and onResume events.
 * @author Florian Otto
 */
class SdkActivityImp : SdkActivity() {
    /**
     * This method has to return the url of the epotheke service, and will be called in
     * SdkActivity during startup.
     *
     * @return
     */
    override fun getCardLinkUrl(): String {
        return serviceUrl + "?token=" + UUID.randomUUID()
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
    override fun getControllerCallback(): CardLinkControllerCallback {
        return ControllerCallbackImp()
    }

    /**
     * This method has to return the SdkErrorHandler which will be used by the sdk to inform about
     * errors of the sdk itself like missing requirements for the functionality etc.
     *
     * @return ControllerCallback
     */
    override fun getSdkErrorHandler(): SdkErrorHandler {
        return object : SdkErrorHandler {
            override fun onError(error: ServiceErrorResponse) {
                setBusy(false)
                when (error.statusCode) {
                    ServiceErrorCode.NFC_NOT_AVAILABLE,
                    ServiceErrorCode.NFC_NOT_ENABLED -> {
                        showInfo("NFC not available or switched off. Please check the settings in android")
                    }

                    else -> {
                        showInfo(error.errorMessage)
                    }
                }
            }
        }
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
            LOG.debug { "epotheke implementation onCanRequest" }
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
            LOG.debug { "epotheke implementation onPhoneNumberRequest" }
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
            LOG.debug { "epotheke implementation onSmsCodeRequest" }
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
            LOG.debug { "epotheke implementation requestCardInsertion" }
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
            LOG.debug { "epotheke implementation requestCardInsertion with nfcOverlayMessageHandler" }
        }

        /**
         * Gets called, when the communication with the card is over.
         * The app may inform the user that the card is no longer needed.
         */
        override fun onCardInteractionComplete() {
            LOG.debug { "epotheke implementation onCardInteractionComplete" }
        }

        /**
         * Gets called, when the intent from android of a detected card, is consumed by the SDK.
         * The app may inform the user, that the card was detected.
         */
        override fun onCardRecognized() {
            LOG.debug { "epotheke implementation onCardRecognized" }
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
            LOG.debug { "epotheke implementation onCardRemoved" }
        }
    }


    /**
     * The ControllerCallback informs about the start of the CardLink process and returns its results.
     */
    private inner class ControllerCallbackImp : CardLinkControllerCallback {
        /**
         * Called when the process starts.
         * The app may inform the user.
         */
        override fun onStarted() {
            LOG.debug { "epotheke implementation onStarted" }
            showInfo("Process started...")
        }

        /**
         * Called when the connection establishment finishes.
         * The result contains values for further usage in protocols when the connection was
         * successfully established.
         * If something went wrong the result will contain an error.
         */

        override fun onAuthenticationCompletion(
            activationResult: ActivationResult?,
            cardLinkProtocols: Set<CardLinkProtocol>
        ) {
            LOG.debug { "epotheke implementation onAuthenticationCompletion" }
            LOG.debug { (activationResult.toString()) }
            runOnUiThread {
                setBusy(false)
                val resultMsg = StringBuilder().apply {
                    append(if (activationResult?.resultCode == ActivationResultCode.OK) "SUCCESS" else "FAIL")
                    append("\n")
                    if (activationResult?.resultCode == ActivationResultCode.OK) {
                        for (key in activationResult.resultParameterKeys) {
                            append(key)
                            append(": ")
                            append(activationResult.getResultParameter(key))
                            append("\n")
                        }
                    } else {
                        append(activationResult?.errorMessage)
                    }
                }.toString()

                showInfo(resultMsg)

                //based on the provided protocol implementations we can enable further use cases.
                cardLinkProtocols.filterIsInstance<PrescriptionProtocol>().first().also { protocol ->
                    enablePrescriptionProtocol(protocol)
                }

                findViewById<Button>(R.id.btn_cancel).apply {
                    text = "FINISH"
                }
            }
        }
    }

    /**
     * Enable and show button to start ePrescription protocol flow.
     */
    private fun enablePrescriptionProtocol(protocol: PrescriptionProtocol) {
        findViewById<Button>(R.id.btn_getPrescriptions).apply {
            visibility = VISIBLE
            isEnabled = true

            setOnClickListener {
                startPrescriptionFlow(protocol)
            }
        }
    }

    /**
     * Start the ePrescription flow
     * The functions of the protocol are kotlin suspend functions and therefore have to be run in
     * a couroutine scope, which enables us to perform the asynchronous communications in the background of the app.
     * To keep it simple we here just use runBlocking
     */
    private fun startPrescriptionFlow(protocol: PrescriptionProtocol) {
        LOG.debug { "Start action for PrescriptionProtocol" }

        runBlocking {
            setBusy(true)
            try {
                /*
                * Send request for available prescriptions via the PrescriptionProtocol object
                * We can use the default values of the constructor to get everything available.
                */
                val result = protocol.requestPrescriptions(
                    RequestPrescriptionList()
                )

                /*
                 * The answer message contains a list of lists.
                 * Each outer list is associated with a card and contains available prescriptions for it.
                 * The inner lists contains types describing prescriptions.
                 *
                 * For the showcase we simply build a string containing names of these elements from the first list.
                 */
                val text =
                    result.availablePrescriptionLists.first().prescriptionBundleList.joinToString(
                        separator = "\n -",
                        limit = 5,
                    ) { bundle: PrescriptionBundle ->
                        val medication = bundle.arzneimittel
                        medication.medicationItem.joinToString { item ->
                            when (item) {
                                is MedicationCompounding -> item.rezepturname
                                is MedicationFreeText -> item.freitextverordnung
                                is MedicationIngredient -> item.listeBestandteilWirkstoffverordnung.joinToString(limit = 3) { i -> i.wirkstoffname }
                                is MedicationPzn -> item.handelsname
                            }
                        }
                    }

                setBusy(false)
                showInfo("Available prescriptions: \n$text")
                enableSelection(protocol, result)

            } catch (e: Exception) {
                LOG.debug(e) { "Error in request" }
                runOnUiThread() {
                    setBusy(false)
                    showInfo("Error in request ${e.message}")
                }
            }
        }
    }

    /**
     * This method switches the functionality of the button to go on with a redemption.
     */
    private fun enableSelection(protocol: PrescriptionProtocol, available: AvailablePrescriptionLists) {
        LOG.debug { "Enable action for Selection" }
        findViewById<Button>(R.id.btn_getPrescriptions).apply {
            text = "SELECT PRESCRIPTIONS"
            setOnClickListener {
                selectPrescriptions(protocol, available)
            }
        }
    }

    /**
     * This function uses the given protocol and the list of available prescriptions to
     * select all of them.
     *
     */
    private fun selectPrescriptions(protocol: PrescriptionProtocol, available: AvailablePrescriptionLists) {
        /*
         * For this example we build a SelectPrescriptionList object requesting all elements (by prescription IDs) of the first list
         * from the previous answer and set the supplyOptionsType to DELIVERY
         */
        var selection = SelectedPrescriptionList(
            iccsn = available.availablePrescriptionLists.first().iccsn,
            prescriptionIndexList = available.availablePrescriptionLists.first()
                .prescriptionBundleList.map {bundle: PrescriptionBundle -> bundle.prescriptionId},
            supplyOptionsType = SupplyOptionsType.DELIVERY
        )

        /*
         * We send the request via the protocol instance to the service and wait for a confirmation message.
         */
        runBlocking {
            setBusy(true)
            try {
                protocol.selectPrescriptions(selection)
                showInfo("SUCCESS")
                disableEprescriptionFunction()
                setBusy(false)
            } catch (e: Exception) {
                LOG.debug(e) { "Error in request" }
                runOnUiThread {
                    setBusy(false)
                    showInfo("Sth. went wrong")
                }
            }
        }
    }

    /**
     * Deactivates the button for ePrescription functionality
     */
    private fun disableEprescriptionFunction() {
        findViewById<Button>(R.id.btn_getPrescriptions).apply {
            visibility = INVISIBLE
        }
    }

    /**
     * Updates the info label to inform the user.
     *
     * @param text
     */
    fun showInfo(text: String?) {
        runOnUiThread {
            findViewById<TextView>(R.id.statusText).apply {
                this.text = text
            }
        }
    }

    /**
     * Shows inputfield and button to let the user enter data.
     *
     * @param infoText
     * @param defaultValue
     * @param action
     */
    private fun getValueFromUser(infoText: String, defaultValue: String, btnAction: (value: String) -> Unit) {
        runOnUiThread {
            showInfo(infoText)

            val inputField = findViewById<TextView>(R.id.input)
            inputField.text = defaultValue

            findViewById<Button>(R.id.btn_ok).apply {
                setOnClickListener {
                    setInputActive(false)
                    btnAction(inputField.text.toString())
                }
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
            findViewById<TextView>(R.id.input).apply {
                visibility = if (active) View.VISIBLE else View.INVISIBLE
            }
            findViewById<Button>(R.id.btn_ok).apply {
                visibility = if (active) View.VISIBLE else View.INVISIBLE
            }
            setBusy(!active)
        }
    }

    /**
     * Shows and hides busy indicator.
     *
     * @param busy
     */
    private fun setBusy(busy: Boolean) {
        findViewById<ProgressBar>(R.id.busy).apply {
            visibility = if (busy) View.VISIBLE else View.INVISIBLE
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.epo_layout)
        findViewById<TextView>(R.id.service).apply {
            text = """
                ${this.text}
                $serviceUrl
                """.trimIndent()
        }

        findViewById<TextView>(R.id.input).apply {
            isEnabled = true
        }
        setInputActive(false)
        findViewById<Button>(R.id.btn_cancel).apply {
            setOnClickListener { finish() }
        }
        super.onCreate(savedInstanceState)
    }

}
