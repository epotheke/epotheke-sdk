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

import CheckBox from 'expo-checkbox';

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

    // This is to toggle whether or not a hard-coded tenantToken is send as auth token
    const [useTenantToken, setUseTenantToken] = useState(false);
    const [useInvalidTenantToken, setUseInvalidTenantToken ] = useState(false);
    const [useRevokedTenantToken, setUseRevokedTenantToken ] = useState(false);
    const [useExpiredTenantToken, setUseExpiredTenantToken ] = useState(false);

    //Use staging default dev
    const [useStaging, setUseStaging] = useState(false);
    const [envUrl, setEnvUrl] = useState(`https://service.dev.epotheke.com/cardlink`)
    const toggleModalVisibility = () => {
        setModalVisible(!isModalVisible);
    };

    async function abortCL() {
        SdkModule.abortCardLink()
        toggleModalVisibility();
    }
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
            setInputValue('+49');
            toggleModalVisibility();
        };
        SdkModule.set_cardlinkInteractionCB_onPhoneNumberRequest(onPhoneNumberRequestCB);

        let onPhoneNumberRetryCB = (code: String | undefined, msg: String | undefined) => {
            log('onPhoneNumberRetryCB');
            SdkModule.set_cardlinkInteractionCB_onPhoneNumberRetry(onPhoneNumberRetryCB);
            setmodalTxt('Retry Number due to: ' + code + ' - ' + msg);
            setInputValue('+49');
            toggleModalVisibility();
        };
        SdkModule.set_cardlinkInteractionCB_onPhoneNumberRetry(onPhoneNumberRetryCB);

        let onSmsCodeRequestCB = () => {
            log('onSmsCodeRequest');
            SdkModule.set_cardlinkInteractionCB_onSmsCodeRequest(onSmsCodeRequestCB);
            setmodalTxt('Provide TAN');
            setInputValue('');
            toggleModalVisibility();
        };
        SdkModule.set_cardlinkInteractionCB_onSmsCodeRequest(onSmsCodeRequestCB);

        let onSmsCodeRetryCB = (code: String | undefined, msg: String | undefined) => {
            log('onSmsCodeRetryCB');
            SdkModule.set_cardlinkInteractionCB_onSmsCodeRetry(onSmsCodeRetryCB);
            setmodalTxt('Retry TAN due to: ' + code + ' - ' + msg);
            setInputValue('');
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
                log(`onAuthenticationCompletion status: ${code} ${msg}`);
            } else {
                try {
                    log(`onAuthenticationCompletion successfull`);

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
        //When the environment allows unauthenticated connection, TENANTTOKEN can be null

        const tenantTokenDEV = "eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiYzcyNGFkMTktZmJmYy00MmFlLThlZDYtN2IzMDgxNDIyNzI5IiwiaWF0IjoxNzMwMjEyODQ3LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTc5MzI4NDg0NywianRpIjoiZGQyN2ZhYmQtMGNmNC00MGVkLThkNjQtMGUzNzlmZWRiMDhiIn0.xD2KqPFaLaXCDm0PO2nvhNFLOxsOqgTq1Np9PqQCmho3StAMjrrp6W1PWQbbxgtCFBY_g5j6y7eKhAx7oUpX0g"
        const tenantTokenDEV_invalid = "eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI7IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiYzcyNGFkMTktZmJmYy00MmFlLThlZDYtN2IzMDgxNDIyNzI5IiwiaWF0IjoxNzMwMjEyODQ3LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTc5MzI4NDg0NywianRpIjoiZGQyN2ZhYmQtMGNmNC00MGVkLThkNjQtMGUzNzlmZWRiMDhiIn0.xD2KqPFaLaXCDm0PO2nvhNFLOxsOqgTq1Np9PqQCmho3StAMjrrp6W1PWQbbxgtCFBY_g5j6y7eKhAx7oUpX0g"
        const tenantTokenDEV_revoked = "eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiYzcyNGFkMTktZmJmYy00MmFlLThlZDYtN2IzMDgxNDIyNzI5IiwiaWF0IjoxNzMwMzY1NzgxLCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTc5MzQzNzc4MSwianRpIjoiYTkxNGQxMGItYmI0NS00NDcyLTg0NWUtYzZiNTNiOTNiNjhmIn0.en-cBlvd5jO0Nz2kuj7dPNFH5xlzPd9TLQZLjxdBkiSfRlV9-i060zO3emUhN8tgSU5ZmwlcGF1sRJLbwJSyPg"
        const tenantTokenDEV_expired = "eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiYzcyNGFkMTktZmJmYy00MmFlLThlZDYtN2IzMDgxNDIyNzI5IiwiaWF0IjoxNzMwMzY2MDc5LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTczMDM2NjM3OSwianRpIjoiNTk3MDFkMTktMjEwNC00OGI0LWI2ZDQtOWQ0ZDhmNmIxZmVjIn0.9Wqj4YMAV18Lfm6v5SdcI8dlGAuqA8TsAuTyDXt5IBKEZaI1OWBq_RdxwP78nD_9H3eX8VgL_9EJ5VpvEWyn4g"

        const tenantTokenSTAGING = "eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiMDE5MmRlMjktMjZhMi03MDAwLTkyMjAtMGFlMDU4YWY2NjE0IiwiaWF0IjoxNzMwMzA0MTE0LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTc5MzM3NjExNCwianRpIjoiNGFjMjExN2MtZWVmMC00ZGU1LWI0YTAtMDQ0YjEwMGViNDM3In0.ApEv-ThtB1Z3UbXZoRDpP5YPIM3kIqGGat5qXwPGxhsvT-w5lokaca4w3G_8lmTgZ_FSXCksudOCXhTf2bw6wA"
        const tenantTokenSTAGING_invalid = "eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI7IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiMDE5MmRlMjktMjZhMi03MDAwLTkyMjAtMGFlMDU4YWY2NjE0IiwiaWF0IjoxNzMwMzA0MTE0LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTc5MzM3NjExNCwianRpIjoiNGFjMjExN2MtZWVmMC00ZGU1LWI0YTAtMDQ0YjEwMGViNDM3In0.ApEv-ThtB1Z3UbXZoRDpP5YPIM3kIqGGat5qXwPGxhsvT-w5lokaca4w3G_8lmTgZ_FSXCksudOCXhTf2bw6wA"
        const tenantTokenSTAGING_revoked = "eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiMDE5MmRlMjktMjZhMi03MDAwLTkyMjAtMGFlMDU4YWY2NjE0IiwiaWF0IjoxNzMwMzY1ODk3LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTc5MzQzNzg5NywianRpIjoiMTJiZWYxMmYtMDYyYS00NTdlLWJmNzAtOGZkZGM5ZDFkYzg1In0.IkrGzjESTE0tCPgqoAklXHKW4jYfzcUDtMR8h97NtJw5X0jYfy_l_K_jhFIXDHav8LhJ1esqwVb4yWOvqmY91Q"
        const tenantTokenSTAGING_expired = "eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiMDE5MmRlMjktMjZhMi03MDAwLTkyMjAtMGFlMDU4YWY2NjE0IiwiaWF0IjoxNzMwMzY2MjMxLCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTczMDM2NjUzMSwianRpIjoiMmQ0Mzc5MDEtYWYwNC00MDQ2LWE0M2UtOWEwNjUxNDJhZDVjIn0._pnFl3myeBhYmlRbIU6dIBqG685IyUdbo8aOUrDqbyLZVQtqPT1t179TIcRfUYBcWjGekxiZpv_CMde2BXEDpA"

        if(useTenantToken){
            if(useStaging){
                SdkModule.startCardLink(envUrl, tenantTokenSTAGING);
            } else {
                SdkModule.startCardLink(envUrl, tenantTokenDEV);
            }
        } else if (useInvalidTenantToken){
            if(useStaging){
                SdkModule.startCardLink(envUrl, tenantTokenSTAGING_invalid);
            } else {
                SdkModule.startCardLink(envUrl, tenantTokenDEV_invalid);
            }
        } else if (useRevokedTenantToken){
            if(useStaging){
               SdkModule.startCardLink(envUrl, tenantTokenSTAGING_revoked);
            } else {
               SdkModule.startCardLink(envUrl, tenantTokenDEV_revoked);
            }
        } else if (useExpiredTenantToken){
            if(useStaging){
               SdkModule.startCardLink(envUrl, tenantTokenSTAGING_expired);
            } else {
               SdkModule.startCardLink(envUrl, tenantTokenDEV_expired);
            }
        } else {
            SdkModule.startCardLink(envUrl, null);
        }
    }

    const log = (msg: string) => {
        console.log(msg);
        setStatus('Status: ' + msg);
    };

    return (
        <SafeAreaView style={styles.view}>
            <ScrollView style={styles.view} contentInsetAdjustmentBehavior="automatic">
                <View style={styles.view}>
                    <View style={styles.space} style={styles.button}/>
                    <Button title="EPOTHEKE" onPress={doCL} />
                    <Text>Url: {envUrl}</Text>
                    <View style={styles.space} />
                    <Text>Switch to staging environment (deafult dev):</Text>
                    <CheckBox
                        style={styles.cb}
                        disabled={false}
                        value={useStaging}
                        onValueChange={(v) => {
                            setUseStaging(v)
                            if(v){
                                setEnvUrl(`https://service.staging.epotheke.com/cardlink`)
                            } else {
                                setEnvUrl(`https://service.dev.epotheke.com/cardlink`)
                            }
                        }}
                    />
                    <View style={styles.space} />
                    <Text>Use valid tenantToken:</Text>
                    <CheckBox
                        style={styles.cb}
                        disabled={false}
                        value={useTenantToken}
                        onValueChange={(v) => {
                            setUseTenantToken(v)
                            if(v){
                                setUseInvalidTenantToken(!v)
                                setUseRevokedTenantToken(!v)
                                setUseExpiredTenantToken(!v)
                            }
                        }}
                    />
                    <Text>Invalid tenantToken:</Text>
                    <CheckBox
                        style={styles.cb}
                        disabled={false}
                        value={useInvalidTenantToken}
                        onValueChange={(v) => {
                            setUseInvalidTenantToken(v)
                            if(v){
                                setUseTenantToken(!v)
                                setUseRevokedTenantToken(!v)
                                setUseExpiredTenantToken(!v)
                            }
                        }}
                    />
                    <Text>Revoked tenantToken:</Text>
                    <CheckBox
                        style={styles.cb}
                        disabled={false}
                        value={useRevokedTenantToken}
                        onValueChange={(v) => {
                            setUseRevokedTenantToken(v)
                            if(v){
                                setUseTenantToken(!v)
                                setUseInvalidTenantToken(!v)
                                setUseExpiredTenantToken(!v)
                            }
                        }}
                    />
                     <Text>Expired tenantToken:</Text>
                     <CheckBox
                         style={styles.cb}
                         disabled={false}
                         value={useExpiredTenantToken}
                         onValueChange={(v) => {
                             setUseExpiredTenantToken(v)
                             if(v){
                                 setUseTenantToken(!v)
                                 setUseInvalidTenantToken(!v)
                                 setUseRevokedTenantToken(!v)
                             }
                         }}
                     />
                    <View style={styles.space} />
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
                                <Button title="Abort" onPress={abortCL} />
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
    button: {
      margin: 20,
    },
    space: {
      width: 20,
      height: 20,
    },
    cb: {
      marginLeft: 20,
    }
});

export default App;
