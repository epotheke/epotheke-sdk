/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import React, {useEffect, useState} from 'react';

import {
    Button,
    Dimensions,
    Modal,
    NativeModules,
    SafeAreaView,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    View,
    Appearance,
} from 'react-native';

import uuid from 'react-native-uuid';

const {SdkModule} = NativeModules;
const {width} = Dimensions.get('window');

function App(): React.JSX.Element {
    useEffect(() => Appearance.setColorScheme('light'), []);
    const [status, setStatus] = useState('Status: not started yet');
    const [modalTxt, setmodalTxt] = useState('Text');

    // This is to manage Modal State
    const [isModalVisible, setModalVisible] = useState(false);

    // This is to manage TextInput State
    const [inputValue, setInputValue] = useState('');

    const toggleModalVisibility = () => {
        setModalVisible(!isModalVisible);
    };

    async function doCL() {
        /*
          Register callbacks for CardLink interaction.
          These are called by the framework during CardLink establishment, to inform app and user about the current state of the process and
          get information like tan, can etc.

          Callbacks in react native can only be called once.
          However, the framework might call any of these multiple times.
          For example, if a given TAN was not correct.
          To allow this behaviour each callback, reregisters itself after execution.
        */
        let requestCardInsertionCB = () => {
            log('requestCardInsertion');
            //reregister callback
            SdkModule.set_cardlinkInteractionCB_requestCardInsertion(requestCardInsertionCB);
        };
        //register callback
        SdkModule.set_cardlinkInteractionCB_requestCardInsertion(requestCardInsertionCB);

        let onCardInteractionComplete = () => {
            log('onCardInteractionComplete');
            SdkModule.set_cardlinkInteractionCB_onCardInteractionComplete(onCardInteractionComplete);
        };
        SdkModule.set_cardlinkInteractionCB_onCardInteractionComplete(onCardInteractionComplete);

        let onCardRecognizedCB = () => {
            log('onCardRecognized');
            SdkModule.set_cardlinkInteractionCB_onCardRecognized(onCardRecognizedCB);
        };
        SdkModule.set_cardlinkInteractionCB_onCardRecognized(onCardRecognizedCB);

        let onCardRemovedCB = () => {
            log('onCardRemoved');
            SdkModule.set_cardlinkInteractionCB_onCardRemoved(onCardRemovedCB);
        };
        SdkModule.set_cardlinkInteractionCB_onCardRemoved(onCardRemovedCB);

        let canRequestCB = () => {
            log('onCanRequest');
            SdkModule.set_cardlinkInteractionCB_onCanRequest(canRequestCB);
            setInputValue('123123');
            setmodalTxt('Provide CAN');
            toggleModalVisibility();
        };
        SdkModule.set_cardlinkInteractionCB_onCanRequest(canRequestCB);

        let canRetryCB = (code: String | undefined, msg: String | undefined) => {
            log('canRetryCB');
            SdkModule.set_cardlinkInteractionCB_onCanRetry(canRetryCB);
            setInputValue('123123');
            setmodalTxt('Retry Can due to: ' + code + ' - ' + msg);
            toggleModalVisibility();
        };
        SdkModule.set_cardlinkInteractionCB_onCanRetry(canRetryCB);

        let onPhoneNumberRequestCB = () => {
            log('onPhoneNumberRequest');
            SdkModule.set_cardlinkInteractionCB_onPhoneNumberRequest(onPhoneNumberRequestCB);
            setmodalTxt('Provide phone number');
            setInputValue('015111122233');
            toggleModalVisibility();
        };
        SdkModule.set_cardlinkInteractionCB_onPhoneNumberRequest(onPhoneNumberRequestCB);

        let onPhoneNumberRetryCB = (code: String | undefined, msg: String | undefined) => {
            log('onPhoneNumberRetryCB');
            SdkModule.set_cardlinkInteractionCB_onPhoneNumberRetry(onPhoneNumberRetryCB);
            setmodalTxt('Retry Number due to: ' + code + ' - ' + msg);
            setInputValue('015111122233');
            toggleModalVisibility();
        };
        SdkModule.set_cardlinkInteractionCB_onPhoneNumberRetry(onPhoneNumberRetryCB);

        let onSmsCodeRequestCB = () => {
            log('onSmsCodeRequest');
            SdkModule.set_cardlinkInteractionCB_onSmsCodeRequest(onSmsCodeRequestCB);
            setmodalTxt('Provide TAN');
            setInputValue('123456789');
            toggleModalVisibility();
        };
        SdkModule.set_cardlinkInteractionCB_onSmsCodeRequest(onSmsCodeRequestCB);

        let onSmsCodeRetryCB = (code: String | undefined, msg: String | undefined) => {
            log('onSmsCodeRetryCB');
            SdkModule.set_cardlinkInteractionCB_onSmsCodeRetry(onSmsCodeRetryCB);
            setmodalTxt('Retry TAN due to: ' + code + ' - ' + msg);
            setInputValue('123456789');
            toggleModalVisibility();
        };
        SdkModule.set_cardlinkInteractionCB_onSmsCodeRetry(onSmsCodeRetryCB);


        /*
          Called if the sdk runs into an error.
        */
        let sdkErrorCB = (code: String | undefined, msg: String | undefined) => {
            log(`sdkError: ${code} - msg: ${msg}`);
            SdkModule.set_sdkErrorCB(sdkErrorCB);
        };
        SdkModule.set_sdkErrorCB(sdkErrorCB);

        /*
          Wiring of the controllerCallbacks
        */
        //this callback informs about the start of the CardLink establishment
        let controllerCallback = () => {
            log('onStarted');
            SdkModule.set_controllerCallbackCB_onStarted(controllerCallback);
        };
        SdkModule.set_controllerCallbackCB_onStarted(controllerCallback);

        /*
          This callback is called when the CardLink establishment is finished.

          If successfull the methods
            SdkModule.getPrescriptions();
            SdkModule.selectPrescriptions();
          become functional and can be called.
        */
        let onAuthenticationCallback = async (code: String | undefined, msg: String | undefined) => {
            log(`authcallback`);
            if(code){
                log(`onAuthenticationCompletion error: ${code} - ${msg}`);
            } else {
                try {
                    log(`success`);

                    //get available prescriptions
                    let availPrescriptions = await SdkModule.getPrescriptions();
                    log(`prescriptions: ${availPrescriptions}`);

                    ////example for a selection
                    ////which has to be done via a jsonstring containing the selectedPrescriptionList
                    //let confirmation = await SdkModule.selectPrescriptions(`{
                    //      "type": "selectedPrescriptionList",
                    //      "ICCSN": "MTIzNDU2Nzg5",
                    //      "prescriptionIndexList": [
                    //        "160.000.764.737.300.50",
                    //        "160.100.000.000.012.06",
                    //        "160.100.000.000.004.30",
                    //        "160.100.000.000.014.97",
                    //        "160.100.000.000.006.24"
                    //      ],
                    //      "supplyOptionsType": "delivery",
                    //      "messageId": "bad828ad-75fa-4eea-aea5-a3587d95ce4a"
                    //    }`);
                    //log(`selection confirmation: ${confirmation}`);
                } catch (e) {
                    log(`error : ${e}`);
                }
            }

            SdkModule.set_controllerCallbackCB_onAuthenticationCompletion(onAuthenticationCallback);
        };
        SdkModule.set_controllerCallbackCB_onAuthenticationCompletion(onAuthenticationCallback);

        // start the CardLink establishment
        //SdkModule.startCardLink(`https://service.dev.epotheke.com/cardlink?token=${uuid.v4()}`, `TENANTTOKEN`);
        //When the environment allows unauthenticated connection, TENANTTOKEN can be null
        SdkModule.startCardLink(`https://mock.test.epotheke.com/cardlink?token=${uuid.v4()}`, null);
    }

    const log = (msg: string) => {
        console.log(msg);
        setStatus('Status: ' + msg);
    };

    return (
        <SafeAreaView style={styles.view}>
            <ScrollView style={styles.view} contentInsetAdjustmentBehavior="automatic">
                <View style={styles.view}>
                    <Button title="EPOTHEKE" onPress={doCL} />
                    <Text>{status}</Text>
                    <Modal
                        animationType="slide"
                        transparent
                        visible={isModalVisible}
                        presentationStyle="overFullScreen">
                        <View style={styles.viewWrapper}>
                            <View style={styles.modalView}>
                                <Text>{modalTxt}</Text>
                                <TextInput
                                    placeholder="Enter something..."
                                    value={inputValue}
                                    style={styles.textInput}
                                    onChangeText={value => setInputValue(value)}
                                />
                                <Button
                                    title="OK"
                                    onPress={() => {
                                        console.log('Sending ${inputValue} to sdk');
                                        SdkModule.setUserInput(inputValue);
                                        toggleModalVisibility();
                                    }}
                                />
                            </View>
                        </View>
                    </Modal>
                </View>
            </ScrollView>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    view: {
        backgroundColor: 'white',
    },
    sectionContainer: {
        marginTop: 32,
        paddingHorizontal: 24,
    },
    sectionTitle: {
        fontSize: 24,
        fontWeight: '600',
    },
    sectionDescription: {
        marginTop: 8,
        fontSize: 18,
        fontWeight: '400',
    },
    highlight: {
        fontWeight: '700',
    },
    viewWrapper: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
    },
    modalView: {
        alignItems: 'center',
        justifyContent: 'center',
        position: 'absolute',
        top: '50%',
        left: '50%',
        elevation: 5,
        transform: [{translateX: -(width * 0.4)}, {translateY: -90}],
        height: 180,
        width: width * 0.8,
        borderRadius: 2,
        backgroundColor: 'white',
    },
    textInput: {
        width: '80%',
        borderRadius: 5,
        paddingVertical: 8,
        paddingHorizontal: 16,
        borderWidth: 1,
        marginBottom: 8,
    },
});

export default App;
