/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import React, {useEffect, useState, useMemo} from 'react';

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

import Clipboard from '@react-native-clipboard/clipboard';
import AsyncStorage from '@react-native-async-storage/async-storage';

import CheckBox from 'expo-checkbox';
import RadioGroup, {RadioButtonProps} from 'react-native-radio-buttons-group';

import uuid from 'react-native-uuid';

const {SdkModule} = NativeModules;
const {width} = Dimensions.get('window');

const envUrls = {
    'dev' : 'https://service.dev.epotheke.com/cardlink',
    'staging' : 'https://service.staging.epotheke.com/cardlink',
    'prod' : 'https://service.epotheke.com/cardlink',
}

const tenantTokens = {
    'dev': {
        'none':null,
        'valid':"eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiYzcyNGFkMTktZmJmYy00MmFlLThlZDYtN2IzMDgxNDIyNzI5IiwiaWF0IjoxNzMwMjEyODQ3LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTc5MzI4NDg0NywianRpIjoiZGQyN2ZhYmQtMGNmNC00MGVkLThkNjQtMGUzNzlmZWRiMDhiIn0.xD2KqPFaLaXCDm0PO2nvhNFLOxsOqgTq1Np9PqQCmho3StAMjrrp6W1PWQbbxgtCFBY_g5j6y7eKhAx7oUpX0g",
        'invalid':"eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI7IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiYzcyNGFkMTktZmJmYy00MmFlLThlZDYtN2IzMDgxNDIyNzI5IiwiaWF0IjoxNzMwMjEyODQ3LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTc5MzI4NDg0NywianRpIjoiZGQyN2ZhYmQtMGNmNC00MGVkLThkNjQtMGUzNzlmZWRiMDhiIn0.xD2KqPFaLaXCDm0PO2nvhNFLOxsOqgTq1Np9PqQCmho3StAMjrrp6W1PWQbbxgtCFBY_g5j6y7eKhAx7oUpX0g",
        'revoked':"eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiYzcyNGFkMTktZmJmYy00MmFlLThlZDYtN2IzMDgxNDIyNzI5IiwiaWF0IjoxNzMwMzY1NzgxLCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTc5MzQzNzc4MSwianRpIjoiYTkxNGQxMGItYmI0NS00NDcyLTg0NWUtYzZiNTNiOTNiNjhmIn0.en-cBlvd5jO0Nz2kuj7dPNFH5xlzPd9TLQZLjxdBkiSfRlV9-i060zO3emUhN8tgSU5ZmwlcGF1sRJLbwJSyPg",
        'expired':"eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiYzcyNGFkMTktZmJmYy00MmFlLThlZDYtN2IzMDgxNDIyNzI5IiwiaWF0IjoxNzMwMzY2MDc5LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTczMDM2NjM3OSwianRpIjoiNTk3MDFkMTktMjEwNC00OGI0LWI2ZDQtOWQ0ZDhmNmIxZmVjIn0.9Wqj4YMAV18Lfm6v5SdcI8dlGAuqA8TsAuTyDXt5IBKEZaI1OWBq_RdxwP78nD_9H3eX8VgL_9EJ5VpvEWyn4g"
    },
    'staging': {
        'none':null,
        'valid':"eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiMDE5MmRlMjktMjZhMi03MDAwLTkyMjAtMGFlMDU4YWY2NjE0IiwiaWF0IjoxNzMwMzA0MTE0LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTc5MzM3NjExNCwianRpIjoiNGFjMjExN2MtZWVmMC00ZGU1LWI0YTAtMDQ0YjEwMGViNDM3In0.ApEv-ThtB1Z3UbXZoRDpP5YPIM3kIqGGat5qXwPGxhsvT-w5lokaca4w3G_8lmTgZ_FSXCksudOCXhTf2bw6wA",
        'invalid':"eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI7IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiMDE5MmRlMjktMjZhMi03MDAwLTkyMjAtMGFlMDU4YWY2NjE0IiwiaWF0IjoxNzMwMzA0MTE0LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTc5MzM3NjExNCwianRpIjoiNGFjMjExN2MtZWVmMC00ZGU1LWI0YTAtMDQ0YjEwMGViNDM3In0.ApEv-ThtB1Z3UbXZoRDpP5YPIM3kIqGGat5qXwPGxhsvT-w5lokaca4w3G_8lmTgZ_FSXCksudOCXhTf2bw6wA",
        'revoked':"eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiMDE5MmRlMjktMjZhMi03MDAwLTkyMjAtMGFlMDU4YWY2NjE0IiwiaWF0IjoxNzMwMzY1ODk3LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTc5MzQzNzg5NywianRpIjoiMTJiZWYxMmYtMDYyYS00NTdlLWJmNzAtOGZkZGM5ZDFkYzg1In0.IkrGzjESTE0tCPgqoAklXHKW4jYfzcUDtMR8h97NtJw5X0jYfy_l_K_jhFIXDHav8LhJ1esqwVb4yWOvqmY91Q",
        'expired':"eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiMDE5MmRlMjktMjZhMi03MDAwLTkyMjAtMGFlMDU4YWY2NjE0IiwiaWF0IjoxNzMwMzY2MjMxLCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTczMDM2NjUzMSwianRpIjoiMmQ0Mzc5MDEtYWYwNC00MDQ2LWE0M2UtOWEwNjUxNDJhZDVjIn0._pnFl3myeBhYmlRbIU6dIBqG685IyUdbo8aOUrDqbyLZVQtqPT1t179TIcRfUYBcWjGekxiZpv_CMde2BXEDpA"
    },
    'prod': {
        'none':null,
        'valid':"eyJraWQiOiJ0ZW5hbnQtc2lnbmVyLTIwMjQxMTA2IiwiYWxnIjoiRVMyNTYiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiMzBiMWZmZDEtY2ViZC00Mzc4LWE2MGUtZjYwNGU2OTJiNmZjIiwiaWF0IjoxNzMzMzgzNTUwLCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTc5NjQ1NTU1MCwianRpIjoiYTUxOWM5M2QtY2JkMi00MDBkLWE2OTgtZWQzODA1M2VjNmE2In0.NeujvpXY9_LPiqobqHGw3_GslF2upVmI0A-nKiu7yNZn6xvJNB4fgmQOxTAIqd2rB6y7aD_kYQRTt39GIHkZAg",
        'invalid':"eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI7IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiMDE5MmRlMjktMjZhMi03MDAwLTkyMjAtMGFlMDU4YWY2NjE0IiwiaWF0IjoxNzMwMzA0MTE0LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTc5MzM3NjExNCwianRpIjoiNGFjMjExN2MtZWVmMC00ZGU1LWI0YTAtMDQ0YjEwMGViNDM3In0.ApEv-ThtB1Z3UbXZoRDpP5YPIM3kIqGGat5qXwPGxhsvT-w5lokaca4w3G_8lmTgZ_FSXCksudOCXhTf2bw6wA",
        'revoked':"eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiMDE5MmRlMjktMjZhMi03MDAwLTkyMjAtMGFlMDU4YWY2NjE0IiwiaWF0IjoxNzMwMzY1ODk3LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTc5MzQzNzg5NywianRpIjoiMTJiZWYxMmYtMDYyYS00NTdlLWJmNzAtOGZkZGM5ZDFkYzg1In0.IkrGzjESTE0tCPgqoAklXHKW4jYfzcUDtMR8h97NtJw5X0jYfy_l_K_jhFIXDHav8LhJ1esqwVb4yWOvqmY91Q",
        'expired':"eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiMDE5MmRlMjktMjZhMi03MDAwLTkyMjAtMGFlMDU4YWY2NjE0IiwiaWF0IjoxNzMwMzY2MjMxLCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTczMDM2NjUzMSwianRpIjoiMmQ0Mzc5MDEtYWYwNC00MDQ2LWE0M2UtOWEwNjUxNDJhZDVjIn0._pnFl3myeBhYmlRbIU6dIBqG685IyUdbo8aOUrDqbyLZVQtqPT1t179TIcRfUYBcWjGekxiZpv_CMde2BXEDpA"
    }

}



function App(): React.JSX.Element {
    const [status, setStatus] = useState('Status: not started yet');
    const [authenticationOngoing, setAuthenticationOngoing] = useState(false);
    const [modalTxt, setModalTxt] = useState('Text');
    const [logFromSdk, setLogFromSdk] = useState('no log yet');

    useEffect(() => {
        async function get() {
            return await SdkModule.activationActive();
        }
        const intervalId = setInterval(async ()=>{
            var v = await get();
            setAuthenticationOngoing(v);
        }, 500);
        return () => { clearInterval(intervalId) }
    },[]);

    // This is to manage Modal State
    const [isModalVisible, setModalVisible] = useState(false);
    const toggleModalVisibility = () => {
        setModalVisible(!isModalVisible);
    };

    // This is to manage TextInput State
    const [inputValue, setInputValue] = useState('');
    const [inputValueStorageKey, setInputValueStorageKey] = useState(undefined);

    async function storeInputValue(val: string){
        if(inputValueStorageKey){
            await AsyncStorage.setItem(inputValueStorageKey, val);
        }
        setInputValueStorageKey(undefined);
    }
    async function loadInputValue(key: string){
        return await AsyncStorage.getItem(key)
    }

    const env_radioBtn: RadioButtonProps[] = useMemo(()=>([

        {
            id: 'dev',
            label: 'dev',
            color: 'black',
            labelStyle: styles.txtblack,
        },
        {
            id: 'staging',
            label: 'staging',
            color: 'black',
            labelStyle: styles.txtblack,
        },
        {
            id: 'prod',
            label: 'prod',
            color: 'black',
            labelStyle: styles.txtblack,
        }
    ]), []);
    const [selectedEnv, setSelectedEnv] = useState<string>('prod');

    const tenantTokens_radioBtn: RadioButtonProps[] = useMemo(()=>([
        {
            id: 'none',
            label: 'none',
            color: 'black',
            labelStyle: styles.txtblack,
        },
        {
            id: 'valid',
            label: 'valid',
            color: 'black',
            labelStyle: styles.txtblack,
        },
        {
            id: 'invalid',
            label: 'invalid',
            color: 'black',
            labelStyle: styles.txtblack,
        },
        {
            id: 'revoked',
            label: 'revoked',
            color: 'black',
            labelStyle: styles.txtblack,
        },
        {
            id: 'expired',
            label: 'expired',
            color: 'black',
            labelStyle: styles.txtblack,
        },
    ]), []);
    const [selectedTenantTokenId, setSelectedTenantTokenId] = useState<string>('valid');

    // Reusable session to cardlink which allows reusing a validated phonenumber for 15 minutes
    const [wsSession, setWsSession] = useState(null);
    const [reUseWsSession, setReUseWsSession] = useState(true);
    const [useStoredInput, setUseStoredInput ] = useState(false);
    const [loopOnSuccess, setLoopOnSuccess] = useState(false);

    async function abortCL() {
        SdkModule.abortCardLink()
        toggleModalVisibility();
    }

    const [fetchPrescriptionsEnabled, setFetchPrescriptionsEnabled] = useState<boolean>(false);
    async function fetchPrescriptions() {
          //get available prescriptions
          try{
            let availPrescriptions = await SdkModule.getPrescriptions();
            log(`prescriptions: ${availPrescriptions}`);
          } catch (e) {
            log(`error during fetch: ${e}`);
          }
    }

    async function copyLogsToClipboard() {
          //get available prescriptions
          try {
                if(SdkModule.getLog){
                    Clipboard.setString(await SdkModule.getLog());
                    log("Logs copied to clipboard");
                } else {
                    log("only ios");
                }
          } catch (e) {
          }
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

        let onCardInsertedCB = () => {
            log('onCardInserted');
            SdkModule.set_cardlinkInteractionCB_onCardInserted(onCardInsertedCB);
        };
        SdkModule.set_cardlinkInteractionCB_onCardInserted(onCardInsertedCB);

        let onCardInsufficientCB = () => {
            log('onCardInsufficient');
            SdkModule.set_cardlinkInteractionCB_onCardInsufficient(onCardInsufficientCB);
        };
        SdkModule.set_cardlinkInteractionCB_onCardInsufficient(onCardInsufficientCB);

        let onCardRemovedCB = () => {
            log('onCardRemoved');
            SdkModule.set_cardlinkInteractionCB_onCardRemoved(onCardRemovedCB);
        };
        SdkModule.set_cardlinkInteractionCB_onCardRemoved(onCardRemovedCB);

        let canRequestCB = async () => {
            var can = "123123"
            try {
                var c = await loadInputValue("CAN");
                if(!!c){
                    if(useStoredInput){
                        SdkModule.setUserInput(c);
                        SdkModule.set_cardlinkInteractionCB_onCanRequest(canRequestCB);
                        return;
                    }
                    can = c;
                }
            } catch (e) {
                log("could not load " + e)
            }
            log(`onCanRequest - stored: `);
            setInputValueStorageKey("CAN");
            setInputValue(can);
            setModalTxt('Provide CAN');

            toggleModalVisibility();
            SdkModule.set_cardlinkInteractionCB_onCanRequest(canRequestCB);
        };
        SdkModule.set_cardlinkInteractionCB_onCanRequest(canRequestCB);

        let canRetryCB = (code: String | undefined, msg: String | undefined) => {
            log(`canRetryCB`);
            setInputValue('123123');
            setModalTxt('CAN WRONG - Provide correct CAN');
            setInputValueStorageKey("CAN");
            toggleModalVisibility();
            SdkModule.set_cardlinkInteractionCB_onCanRetry(canRetryCB);
        };
        SdkModule.set_cardlinkInteractionCB_onCanRetry(canRetryCB);

        let onPhoneNumberRequestCB = async () => {
            log('onPhoneNumberRequest');

            var phone= "+49";
            try {
                var p = await loadInputValue("PHONE");
                if(!!p){
                    if(useStoredInput){
                        SdkModule.setUserInput(p);
                        SdkModule.set_cardlinkInteractionCB_onPhoneNumberRequest(onPhoneNumberRequestCB);
                        return;
                    }
                    phone = p;
                }
            } catch (e) {
                log("could not load " + e)
            }
            setInputValueStorageKey("PHONE");
            setInputValue(phone);
            setModalTxt('Provide phone number');
            toggleModalVisibility();
            SdkModule.set_cardlinkInteractionCB_onPhoneNumberRequest(onPhoneNumberRequestCB);
        };
        SdkModule.set_cardlinkInteractionCB_onPhoneNumberRequest(onPhoneNumberRequestCB);

        let onPhoneNumberRetryCB = (code: String | undefined, msg: String | undefined) => {
            log('onPhoneNumberRetryCB');
            SdkModule.set_cardlinkInteractionCB_onPhoneNumberRetry(onPhoneNumberRetryCB);
            setModalTxt('Retry Number due to: ' + code + ' - ' + msg);
            setInputValue('+49');
            setInputValueStorageKey("PHONE");
            toggleModalVisibility();
        };
        SdkModule.set_cardlinkInteractionCB_onPhoneNumberRetry(onPhoneNumberRetryCB);

        let onSmsCodeRequestCB = () => {
            log('onSmsCodeRequest');
            SdkModule.set_cardlinkInteractionCB_onSmsCodeRequest(onSmsCodeRequestCB);
            setModalTxt('Provide TAN');
            setInputValue('');
            toggleModalVisibility();
        };
        SdkModule.set_cardlinkInteractionCB_onSmsCodeRequest(onSmsCodeRequestCB);

        let onSmsCodeRetryCB = (code: String | undefined, msg: String | undefined) => {
            log('onSmsCodeRetryCB');
            SdkModule.set_cardlinkInteractionCB_onSmsCodeRetry(onSmsCodeRetryCB);
            setModalTxt('Retry TAN due to: ' + code + ' - ' + msg);
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
            log(`onAuthenticationCompletion`);
            if(SdkModule.getLog){
                setLogFromSdk(await SdkModule.getLog());
            }

            if(code){
                log(`onAuthenticationCompletion status: ${code} ${msg}`);
            } else {
                try {
                    log(`onAuthenticationCompletion successfull`);


                    //store wsSession for later reuse
                    let wsSession = await SdkModule.getWsSessionId()
                    log(`wsSessionID: ${wsSession}`);
                    setWsSession(wsSession);

                    setFetchPrescriptionsEnabled(true)

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

                if(loopOnSuccess){
                    setTimeout(async ()=> {
                        var url = envUrls[selectedEnv];
                        if(reUseWsSession && wsSession){
                            url += `?token=${wsSession}`
                        }
                        const tenantToken = tenantTokens[selectedEnv][selectedTenantTokenId];
                        SdkModule.startCardLink(url, tenantToken);
                    }, 10000);
                }
            }

            SdkModule.set_controllerCallbackCB_onAuthenticationCompletion(onAuthenticationCallback);
        };
        SdkModule.set_controllerCallbackCB_onAuthenticationCompletion(onAuthenticationCallback);

        // start the CardLink establishment
        //When the environment allows unauthenticated connection, TENANTTOKEN can be null

        /*
        The cardlink urls are following the scheme:
        `https://service.ENV.epotheke.com/cardlink`
        ENV can be dev, staging, prod

        After a successfull authentication one can also call
        await SdkModule.getWsSessionId()
        which delivers a connection token, and can append this to the cardlink url as a query parameter.
        `https://service.ENV.epotheke.com/cardlink?token=TOKEN`

        This allows to reuse a once validated session, which means that for 15 minutes a new connection will not require
        a TAN validation. Technically the onPhoneNumberRequest and onSmsCodeRequest callbacks will not be called from the SDK
        After the timeout the SMS validation is again active but without the need to reenter the phonenumber.

        A second consequence of reusing sessions is, that one can register up to ten eGKs within one session.
        And get rescriptions for all of them.

        Tokens which were not returned by an earlier successfull session will be ignored.

        */

        var url = envUrls[selectedEnv];
        if(reUseWsSession && wsSession){
            url += `?token=${wsSession}`
        }
        const tenantToken = tenantTokens[selectedEnv][selectedTenantTokenId];
        SdkModule.startCardLink(url, tenantToken);

    }

    const log = (msg: string) => {
        console.log("js-log: " + msg);
        setStatus('Status: ' + msg);
    };

    return (
        <SafeAreaView style={styles.view}>
            <ScrollView style={styles.view} contentInsetAdjustmentBehavior="automatic">
                <View style={styles.view}>
                    <Text style={styles.header}>Epotheke Test App</Text>
                    <View style={styles.space} style={styles.button}/>
                    <Text style={styles.txtblack}>Environment-URL: {envUrls[selectedEnv]}</Text>
                    <View style={styles.space} />
                    <Text style={styles.txtblack}>Environment:</Text>
                    <RadioGroup
                        layout="row"
                        radioButtons={env_radioBtn}
                        onPress={setSelectedEnv}
                        selectedId={selectedEnv}
                    />
                    <View style={styles.space} />
                    <Text style={styles.txtblack}>Reuse wsSession if exists. Current: {wsSession}</Text>
                    <CheckBox
                        style={styles.cb}
                        disabled={false}
                        value={reUseWsSession}
                        onValueChange={(v) => {
                            setReUseWsSession(v)
                        }}
                    />
                    <View style={styles.space} />
                    <Text style={styles.txtblack}>Use tenantToken:</Text>
                    <RadioGroup
                        layout="row"
                        radioButtons={tenantTokens_radioBtn}
                        onPress={setSelectedTenantTokenId}
                        selectedId={selectedTenantTokenId}
                    />
                    <Text style={styles.txtblack}>Try using user input without asking:</Text>
                    <CheckBox
                        style={styles.cb}
                        disabled={false}
                        value={useStoredInput}
                        onValueChange={(v) => {
                            setUseStoredInput(v);
                        }}
                    />
                    <Text style={styles.txtblack}>Loop on success (use dev since no smstan):</Text>
                    <CheckBox
                        style={styles.cb}
                        disabled={false}
                        value={loopOnSuccess}
                        onValueChange={(v) => {
                            setLoopOnSuccess(v);
                            if(!!v){
                                setUseStoredInput(v);
                            }
                        }}
                    />
                    <View style={styles.space} />
                    <Button title="Establish Cardlink"
                        disabled={authenticationOngoing}
                        onPress={
                           () => {
                               setAuthenticationOngoing(true);
                               doCL();
                           }
                    } />
                    <Button title="Cancel"
                                        disabled={!authenticationOngoing}
                                        onPress={
                                           () => {
                                               SdkModule.abortCardLink()
                                           }
                                    } />
                    <Text style={styles.txtblack}>Authentication: {authenticationOngoing ? 'is ongoing' : 'is not active' }</Text>
                    <View style={styles.space} />
                    <Button
                        title="Fetch prescriptions"
                        disabled={!fetchPrescriptionsEnabled}
                        onPress={fetchPrescriptions}
                    />

                    <View style={styles.space} />
                    <View style={styles.space} />
                    <View style={styles.space} />
                    <Text style={styles.txtblack}>{status}</Text>
                    <Button
                        title="Copy logs"
                        onPress={copyLogsToClipboard}
                    />
                    <TextInput
                        multiline
                        value={logFromSdk}
                    />
                    <Modal
                        animationType="slide"
                        transparent
                        visible={isModalVisible}
                        presentationStyle="overFullScreen">
                        <View style={styles.viewWrapper}>
                            <View style={styles.modalView}>
                                <Text style={styles.txtblack}>{modalTxt}</Text>
                                <TextInput
                                    placeholder="Enter something..."
                                    value={inputValue}
                                    style={styles.textInput}
                                    onChangeText={value => setInputValue(value)}
                                />
                                <Button
                                    title="OK"
                                    onPress={async () => {
                                        log(`Sending ${inputValue} to sdk`);
                                        await storeInputValue(inputValue);
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
    txtblack: {
        color: 'black',
    },
    view: {
        height: '100%',
        width: '100%',
        backgroundColor: 'white',
        color: 'black',
    },
    sectionContainer: {
        marginTop: 32,
        paddingHorizontal: 24,
        color: 'black',
    },
    sectionTitle: {
        fontSize: 24,
        fontWeight: '600',
        color: 'black',
    },
    sectionDescription: {
        marginTop: 8,
        fontSize: 18,
        fontWeight: '400',
        color: 'black',
    },
    highlight: {
        fontWeight: '700',
        color: 'black',
    },
    viewWrapper: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        color: 'black',
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
        color: 'black',
    },
    textInput: {
        width: '80%',
        borderRadius: 5,
        paddingVertical: 8,
        paddingHorizontal: 16,
        borderWidth: 1,
        marginBottom: 8,
        color: 'black',
    },
    button: {
      margin: 20,
      color: 'black'
    },
    header: {
        margin: "auto",
        marginTop: 20,
        fontSize: 24,
        fontWeight: '600',
        color: 'black'
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
