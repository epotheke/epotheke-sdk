/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import React from 'react';
import type {PropsWithChildren} from 'react';
import {
    Button,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  useColorScheme,
NativeModules,
  View,
  TextInput,
} from 'react-native';

import {
  Colors,
  DebugInstructions,
  Header,
  LearnMoreLinks,
  ReloadInstructions,
} from 'react-native/Libraries/NewAppScreen';

type SectionProps = PropsWithChildren<{
  title: string;
}>;

const { SdkModule } = NativeModules;

function Section({children, title}: SectionProps): React.JSX.Element {
  const isDarkMode = useColorScheme() === 'dark';
  return (
    <View style={styles.sectionContainer}>
      <Text
        style={[
          styles.sectionTitle,
          {
            color: isDarkMode ? Colors.white : Colors.black,
          },
        ]}>
        {title}
      </Text>
      <Text
        style={[
          styles.sectionDescription,
          {
            color: isDarkMode ? Colors.light : Colors.dark,
          },
        ]}>
        {children}
      </Text>
    </View>
  );
}

function App(): React.JSX.Element {
  const isDarkMode = useColorScheme() === 'dark';
  const [status, setStatus] = React.useState('Status: not started yet')
  const [can, setCan] = React.useState('Not set yet');

  const backgroundStyle = {
    backgroundColor: isDarkMode ? Colors.darker : Colors.lighter,
  };

  const log = (msg: string) => {
    console.log(msg);
    setStatus('Status: ' + msg);
  };

  return (
    <SafeAreaView style={backgroundStyle}>
      <StatusBar
        barStyle={isDarkMode ? 'light-content' : 'dark-content'}
        backgroundColor={backgroundStyle.backgroundColor}
      />
      <ScrollView
        contentInsetAdjustmentBehavior="automatic"
        style={backgroundStyle}>
        <Header />
        <View
          style={{
            backgroundColor: isDarkMode ? Colors.black : Colors.white,
          }}>
          <Button
            title="EPOTHEKE"
            onPress={async () => {

              log(`btn epotheke pressed`)

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
                log(`requestCardInsertion`)
                //reregister callback
                SdkModule.set_cardlinkInteractionCB_requestCardInsertion(requestCardInsertionCB)
              }
              //register callback
              SdkModule.set_cardlinkInteractionCB_requestCardInsertion(requestCardInsertionCB)
      
              let onCardInteractionComplete = () => {
                log(`onCardInteractionComplete`)
                SdkModule.set_cardlinkInteractionCB_onCardInteractionComplete(onCardInteractionComplete)
              }
              SdkModule.set_cardlinkInteractionCB_onCardInteractionComplete(onCardInteractionComplete)
      
              let onCardRecognizedCB = () => {
                log(`onCardRecognized`)
                SdkModule.set_cardlinkInteractionCB_onCardRecognized(onCardRecognizedCB)
              }
              SdkModule.set_cardlinkInteractionCB_onCardRecognized(onCardRecognizedCB)
      
              let onCardRemovedCB = () => {
                log(`onCardRemoved`)
                SdkModule.set_cardlinkInteractionCB_onCardRemoved(onCardRemovedCB)
              }
              SdkModule.set_cardlinkInteractionCB_onCardRemoved(onCardRemovedCB)
      
              let canRequestCB = () => {
                log(`onCanRequest` + can)
                //to give back data alsways use setUserInput
                console.log(typeof can);
                SdkModule.setUserInput(can)
                SdkModule.set_cardlinkInteractionCB_onCanRequest(canRequestCB)
              }
              SdkModule.set_cardlinkInteractionCB_onCanRequest(canRequestCB)
      
              let onPhoneNumberRequestCB = () => {
                log(`onPhoneNumberRequest`)
                SdkModule.setUserInput("+4915123456789")
                SdkModule.set_cardlinkInteractionCB_onPhoneNumberRequest(onPhoneNumberRequestCB)
              }
              SdkModule.set_cardlinkInteractionCB_onPhoneNumberRequest(onPhoneNumberRequestCB)
      
              let onSmsCodeRequestCB = () => {
                log(`onSmsCodeRequest`)
                SdkModule.setUserInput("123456")
                SdkModule.set_cardlinkInteractionCB_onSmsCodeRequest(onSmsCodeRequestCB)
              }
              SdkModule.set_cardlinkInteractionCB_onSmsCodeRequest(onSmsCodeRequestCB)
      
              /*
                Called if the sdk runs into an error.
              */
              let sdkErrorCB = (err: any, msg: any) => {
                log(`sdkError: ${msg}`)
                SdkModule.set_sdkErrorCB(sdkErrorCB)
              }
              SdkModule.set_sdkErrorCB(sdkErrorCB)
      
      
              /*
                Wiring of the controllerCallbacks
              */
              //this callback informs about the start of the CardLink establishment
              let controllerCallback = () => {
                log(`onStarted`)
                SdkModule.set_controllerCallbackCB_onStarted(controllerCallback)
              }
              SdkModule.set_controllerCallbackCB_onStarted(controllerCallback)
      
              /*
                This callback is called when the CardLink establishment is finished.
      
                If successfull the methods
                  SdkModule.getPrescriptions()
                  SdkModule.selectPrescriptions()
                become functional and can be called.
              */
              let onAuthenticationCallback = async (err: any, msg: any) => {
                  log(`onAuthenticationCompletion error: ${err}`)
                  log(`onAuthenticationCompletion protos: ${msg}`)
      
                  try {
                    //get available prescriptions
                    let availPrescriptions = await SdkModule.getPrescriptions();
                    log(`prescriptions: ${availPrescriptions}`)
      
                    //example for a selection
                    //which has to be done via a jsonstring containing the selectedPrescriptionList
                    let confirmation = await SdkModule.selectPrescriptions(`{
                      "type": "selectedPrescriptionList",
                      "ICCSN": "MTIzNDU2Nzg5",
                      "prescriptionIndexList": [
                        "160.000.764.737.300.50",
                        "160.100.000.000.012.06",
                        "160.100.000.000.004.30",
                        "160.100.000.000.014.97",
                        "160.100.000.000.006.24"
                      ],
                      "supplyOptionsType": "delivery",
                      "messageId": "bad828ad-75fa-4eea-aea5-a3587d95ce4a"
                    }`);
                    log(`selection confirmation: ${confirmation}`)
      
                  } catch (e) {
                    log(`error : ${e}`)
                  }
      
                  SdkModule.set_controllerCallbackCB_onAuthenticationCompletion(onAuthenticationCallback)
              }
              SdkModule.set_controllerCallbackCB_onAuthenticationCompletion(onAuthenticationCallback)
      
      
              // start the CardLink establishment
              SdkModule.startCardLink(`https://mock.test.epotheke.com/cardlink?token=RANDOMTOKEN`)

            }} />
          <TextInput 
            style={{borderWidth: 1, margin: 10}}
            onChangeText={newText => setCan(newText)}
            placeholder="Enter your CAN"
            keyboardType='numeric'
          ></TextInput>
          <Text style={{textAlign: 'center', marginVertical: 10}}>CAN: {can}</Text>
          <Text style={{textAlign: 'center', marginVertical: 10}}>{status}</Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
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
});

export default App;
