/****************************************************************************
 * Copyright (C) 2017-2024 ecsec GmbH.
 * All rights reserved.
 * Contact: ecsec GmbH (info@ecsec.de)
 *
 * This file is part of Epotheke
 *
 * Epotheke is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Epotheke is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Epotheke.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Other Usage
 * Alternatively, this file may be used in accordance with the terms
 * and conditions contained in a signed written agreement between
 * you and ecsec GmbH.
 *
 ***************************************************************************/

package com.epotheke.demo;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.epotheke.sdk.CardLinkProtocol;
import com.epotheke.sdk.EpothekeActivity;

import org.openecard.mobile.activation.ActivationResult;
import org.openecard.mobile.activation.ActivationResultCode;
import org.openecard.mobile.activation.CardLinkInteraction;
import org.openecard.mobile.activation.ConfirmPasswordOperation;
import org.openecard.mobile.activation.ConfirmTextOperation;
import org.openecard.mobile.activation.ControllerCallback;
import org.openecard.mobile.activation.NFCOverlayMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author Florian Otto
 */
public class EpothekeActivityImp extends EpothekeActivity {

    private static final Logger LOG = LoggerFactory.getLogger(EpothekeActivityImp.class);
    private static final String mockServiceUrl = "https://epotheke.mock.ecsec.services/cardlink";

    /**
     * This method has to return the url of the epotheke service.
     *
     * @return
     */
    @NonNull
    @Override
    public String getCardlinkUrl() {
        return mockServiceUrl + "?token=" + UUID.randomUUID();
    }

    /**
     * This method has to return the CardLinkInteraction which contains handlers for different
     * steps in the cardlink process, which have to be implemented by the app.
     *
     * @return CardLinkInteraction
     */
    @NonNull
    @Override
    public CardLinkInteraction getCardLinkInteraction() {
        return new CardLinkInteractionImp();
    }

    /**
     * This method has to return the ControllerCallback which contains events for the start and the
     * end of the link establishment
     *
     * @return ControllerCallback
     */
    @NonNull
    @Override
    public ControllerCallback getControllerCallback() {
        return new ControllerCallbackImp();
    }

    /**
     * This method has to return protocols which contain implememntations for processes after
     * a link is established.
     *
     * @return Set<CardLinkProtocol>
     */
    @NonNull
    @Override
    public Set<CardLinkProtocol> getProtocols() {
        return new HashSet<CardLinkProtocol>();
    }

    /**
     * Implementation of the CardLinkInteraction
     * The different methods get called during the interaction with the card and may require
     * the user to interact with the device, for example to enter a TAN etc.
     */
    private class CardLinkInteractionImp implements CardLinkInteraction {

        /**
         * Called when the SDK communications with the card.
         * The user must read the CAN from it card and enter it within the app.
         * The given CAN has then be handed back to the SDK via
         * confirmPasswordOperation.confirmPassowrd("<TANVALUE>")
         *
         * @param confirmPasswordOperation
         */
        @Override
        public void onCanRequest(ConfirmPasswordOperation confirmPasswordOperation) {
            LOG.debug("EpothekeImplementation onCanRequest");
            getValueFromUser("Please provide CAN of card", "000000", (String val) -> {
                confirmPasswordOperation.confirmPassword(val);
            });
        }


        /**
         * Called during the cardlink establishment.
         * The service needs the number of the users device to send an sms for verification.
         * The app should ask the user to enter a number, which is then handed back to the SDK via
         * confirmTextOperation.confirmText("<NUMBER>");
         *
         * @param confirmTextOperation
         */
        @Override
        public void onPhoneNumberRequest(ConfirmTextOperation confirmTextOperation) {
            LOG.debug("EpothekeImplementation onPhoneNumberRequest");
            getValueFromUser("Please provide valid german phonenumber (mock won't send sms)", "+4915123456789", (String val) -> {
                confirmTextOperation.confirmText(val);
            });
        }

        /**
         * Called during the cardlink establishment.
         * The user will get an SMS containing a TAN for verification purposes.
         * The given TAN has then be handed back to the SDK via
         * confirmPasswordOperation.confirmPassowrd("<TANVALUE>")
         *
         * @param confirmPasswordOperation
         */
        @Override
        public void onSmsCodeRequest(ConfirmPasswordOperation confirmPasswordOperation) {
            LOG.debug("EpothekeImplementation onSmsCodeRequest");
            getValueFromUser("Please provide TAN from sms (mock accepts all)", "123456", (String val) -> {
                confirmPasswordOperation.confirmPassword(val);
            });
        }

        /**
         * Called during the cardlink establishment, when the SDK has to communicate with the card.
         * The method is called to enable the app to inform the user, to bring the card to
         * the device.
         */
        @Override
        public void requestCardInsertion() {
            LOG.debug("EpothekeImplementation requestCardInsertion");
            runOnUiThread(() -> {
                setBusy(false);
                showInfo("Please provide card");
            });
        }

        /**
         * This is not called in android and used only in iOS.
         *
         * @param nfcOverlayMessageHandler
         */
        @Override
        public void requestCardInsertion(NFCOverlayMessageHandler nfcOverlayMessageHandler) {
            LOG.debug("EpothekeImplementation requestCardInsertion with nfcOverlayMessageHandler");
        }

        /**
         * Gets called, when the communication with the card is over.
         * The app may inform the user.
         */
        @Override
        public void onCardInteractionComplete() {
            LOG.debug("EpothekeImplementation onCardInteractionComplete");
        }

        /**
         * Gets called, when the intent from android with a detected card, is consumed by the SDK.
         * The app may inform the user.
         */
        @Override
        public void onCardRecognized() {
            LOG.debug("EpothekeImplementation onCardRecognized");
            runOnUiThread(() -> {
                setBusy(true);
                showInfo("Found card - please don't move card and device");
            });
        }

        /**
         * Gets called, when the card is removed from the reader.
         * The app may inform the user.
         */
        @Override
        public void onCardRemoved() {
            LOG.debug("EpothekeImplementation onCardRemoved");
        }
    }


    /**
     * The ControllerCallback informs about the start of the CardLink process and returns results.
     */
    private class ControllerCallbackImp implements ControllerCallback {

        /**
         * Called when the process starts.
         */
        @Override
        public void onStarted() {
            LOG.debug("EpothekeImplementation onStarted");
            showInfo("Process started...");
        }

        /**
         * Called when the process finishes.
         * The Result contains values for further usage in Protocols when the connection was
         * successfully.
         * If something went wrong the Result will contain an error.
         */
        @Override
        public void onAuthenticationCompletion(ActivationResult activationResult) {
            LOG.debug("EpothekeImplementation onAuthenticationCompletion");
            LOG.debug(activationResult.toString());
            runOnUiThread(() -> {
                setBusy(false);
                StringBuilder sb = new StringBuilder();
                sb.append(activationResult.getResultCode().equals(ActivationResultCode.OK) ? "SUCCESS" : "FAIL");
                sb.append("\n");
                if (activationResult.getResultCode().equals(ActivationResultCode.OK)) {
                    for (String key : activationResult.getResultParameters().keySet()) {
                        sb.append(key);
                        sb.append(": ");
                        sb.append(activationResult.getResultParameters().get(key));
                        sb.append("\n");
                    }
                } else {
                    sb.append(activationResult.getErrorMessage());
                }
                showInfo(sb.toString());
                Button c = findViewById(R.id.btn_cancel);
                c.setText("FINISH");
            });
        }
    }

    /**
     * Functional interface to be able to bind sdk-callbacks to button taps
     */
    private interface ButtonAction {
        void act(String val);
    }

    /**
     * Updates info label
     * @param text
     */
    public void showInfo(String text) {
        runOnUiThread(() -> {
            TextView t = findViewById(R.id.statusText);
            t.setText(text);
        });
    }

    /**
     * Shows inputfield and button to let the user enter data.
     * @param infoText
     * @param defaultValue
     * @param action
     */
    private void getValueFromUser(String infoText, String defaultValue, ButtonAction action) {
        runOnUiThread(() -> {
            showInfo(infoText);
            TextView inputField = findViewById(R.id.input);
            inputField.setText(defaultValue);
            Button b = findViewById(R.id.btn_ok);
            b.setOnClickListener(v -> {
                setInputActive(false);
                TextView input = findViewById(R.id.input);
                action.act(input.getText().toString());
            });
            setInputActive(true);
        });
    }

    /**
     * Shows and hides input elements
     * @param active
     */
    private void setInputActive(boolean active) {
        runOnUiThread(() -> {
            TextView inputField = findViewById(R.id.input);
            inputField.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
            Button b = findViewById(R.id.btn_ok);
            b.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
            setBusy(!active);
        });
    }

    /**
     * Shows and hides busy indicator.
     * @param busy
     */
    private void setBusy(boolean busy) {
        ProgressBar b = findViewById(R.id.busy);
        b.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        setContentView(R.layout.epo_layout);
        TextView service = findViewById(R.id.service);
        service.setText(service.getText() + "\n" + mockServiceUrl);
        TextView inputField = findViewById(R.id.input);
        inputField.setEnabled(true);
        setInputActive(false);
        Button c = findViewById(R.id.btn_cancel);
        c.setOnClickListener((v) -> finish());
        super.onCreate(savedInstance);
    }

}
