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

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View.VISIBLE
import android.view.View.INVISIBLE
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.epotheke.cardlink.CardLinkErrorCodes
import com.epotheke.cardlink.CardlinkAuthResult
import com.epotheke.cardlink.ResultCode
import com.epotheke.cardlink.UserInteraction
import com.epotheke.erezept.model.MedicationCompounding
import com.epotheke.erezept.model.MedicationFreeText
import com.epotheke.erezept.model.MedicationIngredient
import com.epotheke.erezept.model.MedicationPzn
import com.epotheke.erezept.model.PrescriptionBundle
import com.epotheke.erezept.model.RequestPrescriptionList
import com.epotheke.sdk.Epotheke
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.openecard.sc.pcsc.AndroidTerminalFactory
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.core.content.edit
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger { }

enum class Service(val url: String, val tenantToken: String?) {
    MOCK(
        "https://mock.test.epotheke.com/cardlink", null
    ),
    DEV("https://service.dev.epotheke.com/cardlink", null),

    STAGE(
        "https://service.staging.epotheke.com/cardlink",
        "eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiMDE5MmRlMjktMjZhMi03MDAwLTkyMjAtMGFlMDU4YWY2NjE0IiwiaWF0IjoxNzMwMzA0MTE0LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTc5MzM3NjExNCwianRpIjoiNGFjMjExN2MtZWVmMC00ZGU1LWI0YTAtMDQ0YjEwMGViNDM3In0.ApEv-ThtB1Z3UbXZoRDpP5YPIM3kIqGGat5qXwPGxhsvT-w5lokaca4w3G_8lmTgZ_FSXCksudOCXhTf2bw6wA"
    ),

    PROD(
        "https://service.epotheke.com/cardlink",
        "eyJraWQiOiJ0ZW5hbnQtc2lnbmVyLTIwMjQxMTA2IiwiYWxnIjoiRVMyNTYiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiMDE5M2NlZTMtMTdkOC03MDAwLTkwOTktZmM4NGNlMjYyNzk1IiwiaWF0IjoxNzQxMTczNzM4LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTgwNDI0NTczOCwianRpIjoiYWE2NDA5NWMtY2NlNy00N2FjLWEzZDItYzA2ZThlYjE2MmVmIn0.L0D7XGchxtkv_rzvzvru6t80MJy8aQKhbiTReH69MNBVgp9Z-wUlDgIPdpbySmhDSTVEbp1rCwQAOyXje1dntQ"
    ),
}

class InputStore(activity: EpothekeActivity) {
    val preferences: SharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)

    var phoneNumber: String
        get() = preferences.getString("PHONE_$env", "+4915123456789") ?: "+4915123456789"
        set(v) = preferences.edit { putString("PHONE_$env", v) }

    var can: String
        get() = preferences.getString("CAN_$env", "123123") ?: "123123"
        set(v) = preferences.edit { putString("CAN_$env", v) }

    var env: Service
        get() = preferences.getString("ENV", "MOCK")?.let {
            Service.valueOf(it)
        } ?: Service.MOCK
        set(v) = preferences.edit { putString("ENV", v.name) }
}


class EpothekeActivity : AppCompatActivity() {

    lateinit var storedValues: InputStore

    /**
     * Epotheke uses NFC to communicate with eGK cards.
     * The AndroidTerminalFactory is provided by the used open-ecard library and enables NFC usage.
     */
    private var terminalFactory: AndroidTerminalFactory? = null

    /**
     * The epotheke instance which can be used for authenticating or adding eGK cards to a cardlink session and to
     * request prescriptions for all registered cards in a session.
     *
     * For those use-cases epotheke provides the protocol instances:
     * - cardlinkAuthenticationProtocol
     * - prescriptionProtocol
     * which provide functions to perform those actions.
     */
    private var epotheke: Epotheke? = null

    /**
     * Via epotheke we can add multiple eGKs to a carldink-session.
     * We here store the results of each such authentication process for later use.
     */
    private var authenticationResults: MutableList<CardlinkAuthResult> = mutableListOf()

    private var curService = Service.MOCK

    private var currentJob: Job? = null

    /**
     * To create an instance of epotheke we have to provide:
     * - an instance of the AndroidTerminalFactory
     * - the url to the service
     * - a tenant-token for access
     * - an optional wsSessionId enabling to reconnect to an existing session (can be obtained in an authentication Result)
     *
     * Note that the AndroidTerminalFactory needs a reference to the activity and we also store a referenced of it (see onNewIntent).
     */
    private fun createEpotheke() {

        epotheke = Epotheke(
            AndroidTerminalFactory.instance(this).also { fact ->
                terminalFactory = fact
            }, curService.url, curService.tenantToken, null
        )

        //just for showing the current environment
        findViewById<TextView>(R.id.service).apply {
            text = "Service: ${curService.url}"
        }
    }

    private fun switchEnv(env: Service) {
        setInputActive(false)
        setBusy(false)

        currentJob?.cancel()
        curService = env
        storedValues.env = env
        createEpotheke()
        showInfo("Switched to $env")
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        storedValues = InputStore(this)

        setContentView(R.layout.epo_layout)

        createEpotheke()

        findViewById<TextView>(R.id.input).apply {
            isEnabled = true
        }

        findViewById<RadioButton>(R.id.radBtnMock).apply {
            isChecked = storedValues.env == Service.MOCK
            setOnClickListener {
                switchEnv(Service.MOCK)
            }
        }
        findViewById<RadioButton>(R.id.radBtnDev).apply {
            isChecked = storedValues.env == Service.DEV
            setOnClickListener {
                switchEnv(Service.DEV)
            }
        }
        findViewById<RadioButton>(R.id.radBtnStaging).apply {
            isChecked = storedValues.env == Service.STAGE
            setOnClickListener {
                switchEnv(Service.STAGE)
            }
        }
        findViewById<RadioButton>(R.id.radBtnProd).apply {
            isChecked = storedValues.env == Service.PROD
            setOnClickListener {
                switchEnv(Service.PROD)
            }
        }

        findViewById<Button>(R.id.btn_establishCardlink).apply {
            setOnClickListener {
                currentJob = lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        establishCardlink()
                    }
                }
            }
        }

        findViewById<Button>(R.id.btn_getPrescriptions).apply {
            setOnClickListener {
                currentJob = lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        requestPrescriptions()
                    }
                }
            }
        }

        setInputActive(false)
        findViewById<Button>(R.id.btn_cancel).apply {
            setOnClickListener { finish() }
        }
        setBusy(false)

        super.onCreate(savedInstanceState)
    }


    /**
     *  NFC handling is completely managed in the AndroidTerminalFactory except for the following.
     *  Once NFC is activated and the android OS detects a card it pauses and reactivates this activity
     *  with `onNewIntent` providing a reference to the card.
     *  This intent has to be handed to the AndroidTerminalFactory instance to allow the sdk
     *  communicating with the card.
     *  One can only overwrite this function here, which is why this has to be done in the app, when
     *  integrating epotheke.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        terminalFactory?.tagIntentHandler?.invoke(intent)
    }

    /**
     * It is adviced to call close function to cleanup resources from epotheke.
     */
    public override fun onDestroy() {
        super.onDestroy()
        epotheke?.close()
    }


    /**
     * During the cardlink authentication the SDK will control the process and call different methods
     * to inform the app/user of the current step or ask the user for interaction.
     * Implementing this interface gives the app developer the full control of how these steps are
     * presented to the user or how the information is gathered.
     *
     * Note that there is no guarantee that each method is called, for the sequence or that a method is
     * only called once.
     */
    private val userInteraction = object : UserInteraction {

        /**
         * The service needs the cellphone number of the users device to send an sms containing a TAN for verification.
         *
         * Note: The mock service won't send real sms to the given number but will accept 123123 as a correct TAN.
         * The number must be in a format valid for german mobile numbers to be accepted by the mock service.
         */
        override suspend fun onPhoneNumberRequest() = suspendCoroutine { continuation ->
            getValueFromUser(
                "Please provide valid german phone number (mock won't send sms)", storedValues.phoneNumber
            ) { value ->
                storedValues.phoneNumber = value
                continuation.resume(value)
            }
        }

        /**
         * Called if something went wrong in the onPhoneNumberRequest.
         * Information can be gathered by errCode and errMsg.
         */
        override suspend fun onPhoneNumberRetry(
            resultCode: ResultCode, errorMessage: String?
        ) = suspendCoroutine { continuation ->
            getValueFromUser(
                "Problem with phone number: $resultCode. Please try again", storedValues.phoneNumber
            ) { value ->
                storedValues.phoneNumber = value
                continuation.resume(value)
            }
        }


        /**
         * After the user provided the phoneNumber an SMS containing a TAN gets sent by the service.
         * onTanRequest is called to provide the TAN to the sdk for verification at the service.
         *
         * Note: The mock service will accept 123123 as valid TAN.
         */
        override suspend fun onTanRequest() = suspendCoroutine { continuation ->
            getValueFromUser(
                "Please provide TAN from sms (mock accepts 123123)", "123123"
            ) { value ->
                continuation.resume(value)
            }
        }

        /**
         * Called if something went wrong in onTanRequest.
         * Information can be gathered by errCode and errMsg.
         */
        override suspend fun onTanRetry(
            resultCode: ResultCode, errorMessage: String?
        ) = suspendCoroutine { continuation ->
            getValueFromUser(
                "Problem with TAN: $resultCode. Please provide TAN from sms (mock accepts all)",
                "123123"
            ) { value ->
                continuation.resume(value)
            }
        }

        /**
         * Called when the SDK needs to communicates with the card
         * and needs the CAN to establish NFC channel.
         */
        override suspend fun onCanRequest() = suspendCoroutine { continuation ->
            getValueFromUser("Please provide CAN of card", storedValues.can) { value ->
                storedValues.can = value
                continuation.resume(value)
            }
        }

        /**
         * Called if something went wrong in a previous `onCanRequest`.
         * Information about what went wrong can be gathered by resultCode and message.
         */
        override suspend fun onCanRetry(
            resultCode: CardLinkErrorCodes.ClientCodes, errorMessage: String?
        ) = suspendCoroutine { continuation ->
            getValueFromUser("$resultCode - Please provide CAN of card", storedValues.can) { value ->
                storedValues.can = value
                continuation.resume(value)
            }
        }

        /**
         * Called during the connection establishment, when the SDK has to communicate with the card.
         * The method is called to enable the app to inform the user, to bring the card to
         * the devices nfc sensor.
         */
        override suspend fun requestCardInsertion() = runOnUiThread {
            setBusy(false)
            showInfo("Please provide card")
        }

        /**
         * Gets called, when the intent from android of a detected card, is consumed by the SDK.
         * The app may inform the user, that the card was detected.
         */
        override suspend fun onCardRecognized() = runOnUiThread {
            setBusy(true)
            showInfo("Card was detected. Try to avoid movement.")
        }

        /**
         * Gets called, when the card is not sufficient for the process.
         */
        override suspend fun onCardInsufficient() = runOnUiThread {
            setBusy(false)
            showInfo("Card was insufficient")
        }

        /**
         * Gets called, when the card is removed from the reader.
         * The app may inform the user, that the app was removed.
         */
        override suspend fun onCardRemoved() = runOnUiThread {
            setBusy(false)
            showInfo("Card was removed or connection was lost.")
        }

    }

    /**
     * Shows inputfield and button to let the user enter data.
     *
     * @param infoText
     * @param defaultValue
     * @param action
     */
    fun getValueFromUser(
        infoText: String, defaultValue: String, btnAction: (value: String) -> Unit
    ) {
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
     * Shows and hides input elements
     *
     * @param active
     */
    private fun setInputActive(active: Boolean) {
        runOnUiThread {
            findViewById<TextView>(R.id.input).apply {
                visibility = if (active) VISIBLE else INVISIBLE
            }
            findViewById<Button>(R.id.btn_ok).apply {
                visibility = if (active) VISIBLE else INVISIBLE
            }
            setBusy(!active)
        }
    }

    /**
     * Shows and hides busy indicator.
     *
     * @param busy
     */
    private fun setBusy(busy: Boolean) = runOnUiThread {
        findViewById<ProgressBar>(R.id.busy).apply {
            visibility = if (busy) VISIBLE else INVISIBLE
        }
        listOf(
            findViewById<Button>(R.id.btn_establishCardlink),
            findViewById<Button>(R.id.btn_getPrescriptions),
        ).forEach {
            it.isEnabled = !busy
        }
    }


    /**
     * As mentioned before, epotheke provides an instance of cardlinkAuthenticationProtocol.
     * This can be used to call establishCardlink() function as shown here.
     */
    @OptIn(ExperimentalStdlibApi::class)
    private fun establishCardlink() {
        showInfo("Performing carldink authentication")
        setBusy(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {

                epotheke?.cardlinkAuthenticationProtocol?.establishCardlink(interaction = userInteraction)
                    ?.let { result: CardlinkAuthResult ->
                        authenticationResults.add(result)
                        showInfo("WS-Session: ${result.wsSessionId}\n ICCSN: ${result.iccsn}")
                    }

            } catch (e: Exception) {
                showInfo("Error ${e.message}")
                logger.error(e) { "Exception authenticating" }
            }
            setBusy(false)
        }
    }

    /**
     * As mentioned before, epotheke provides an instance of prescriptionProtocol.
     * This can be used to call requestPrescriptions() function as shown here.
     */
    @OptIn(ExperimentalStdlibApi::class)
    private fun requestPrescriptions() {
        logger.debug { "Start action for PrescriptionProtocol" }

        lifecycleScope.launch(Dispatchers.IO) {
            setBusy(true)
            try {
                /**
                 * Send request for available prescriptions via the PrescriptionProtocol object
                 * We can use the default values of the constructor to get everything available.
                 */
                val result = epotheke?.prescriptionProtocol?.requestPrescriptions(
                    RequestPrescriptionList()
                )

                /**
                 * The answer message contains a list of lists.
                 * Each outer list is associated with a card and contains available prescriptions for it.
                 * The inner lists contains types describing prescriptions.
                 *
                 * For the showcase we simply build a string containing names of these elements from the lists associated witht iccsns.
                 */
                val text = result?.availablePrescriptionLists?.joinToString(separator = "\n") {
                    val meds = it.prescriptionBundleList.joinToString(
                        separator = "\n -",
                        limit = 2,
                    ) { bundle: PrescriptionBundle ->
                        val medication = bundle.arzneimittel
                        medication.medicationItem.joinToString { item ->
                            when (item) {
                                is MedicationCompounding -> item.rezepturname ?: "Unknown"
                                is MedicationFreeText -> item.freitextverordnung
                                is MedicationIngredient -> item.listeBestandteilWirkstoffverordnung.joinToString(
                                    limit = 3
                                ) { i -> i.wirkstoffname }

                                is MedicationPzn -> item.handelsname
                            }
                        }
                    }
                    "${it.iccsn.toHexString()}: \n - $meds"
                }


                setBusy(false)
                showInfo("Available prescriptions: \n$text")

            } catch (e: Exception) {
                logger.debug(e) { "Error in request" }
                setBusy(false)
                showInfo("Error in request ${e.message}")
            }
        }
    }
}
