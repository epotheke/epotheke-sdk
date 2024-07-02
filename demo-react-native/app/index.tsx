import { NativeModules, Button, Text, View } from "react-native";

const { EpothekeModule } = NativeModules;

export default function Index() {
  return (
    <View
      style={{
        flex: 1,
        justifyContent: "center",
        alignItems: "center",
      }}
    >
      <Button title="epotheke" onPress={async () => {

        console.log(`btn epotheke pressed`)

        /*
          Register callbacks for cardlink interaction.
          These are called by the framework during cardlink establishment, to inform app and user about the current state of the process and
          get information like tan, can etc.

          Callbacks in react native can only be called once.
          However, the framework might call any of these multiple times.
          For example, if a given TAN was not correct.
          To allow this behaviour each callback, reregisters itself after execution.
        */
        let requestCardInsertionCB = () => {
          console.log(`requestCardInsertion`)
          //reregister callback
          EpothekeModule.set_cardlinkInteractionCB_requestCardInsertion(requestCardInsertionCB)
        }
        //register callback
        EpothekeModule.set_cardlinkInteractionCB_requestCardInsertion(requestCardInsertionCB)

        let onCardInteractionComplete = () => {
          console.log(`onCardInteractionComplete`)
          EpothekeModule.set_cardlinkInteractionCB_onCardInteractionComplete(onCardInteractionComplete)
        }
        EpothekeModule.set_cardlinkInteractionCB_onCardInteractionComplete(onCardInteractionComplete)

        let onCardRecognizedCB = () => {
          console.log(`onCardRecognized`)
          EpothekeModule.set_cardlinkInteractionCB_onCardRecognized(onCardRecognizedCB)
        }
        EpothekeModule.set_cardlinkInteractionCB_onCardRecognized(onCardRecognizedCB)

        let onCardRemovedCB = () => {
          console.log(`onCardRemoved`)
          EpothekeModule.set_cardlinkInteractionCB_onCardRemoved(onCardRemovedCB)
        }
        EpothekeModule.set_cardlinkInteractionCB_onCardRemoved(onCardRemovedCB)

        let canRequestCB = () => {
          console.log(`onCanRequest`)
          //to give back data alsways use setUserInput
          EpothekeModule.setUserInput("000000")
          EpothekeModule.set_cardlinkInteractionCB_onCanRequest(canRequestCB)
        }
        EpothekeModule.set_cardlinkInteractionCB_onCanRequest(canRequestCB)

        let onPhoneNumberRequestCB = () => {
          console.log(`onPhoneNumberRequest`)
          EpothekeModule.setUserInput("+4915123456789")
          EpothekeModule.set_cardlinkInteractionCB_onPhoneNumberRequest(onPhoneNumberRequestCB)
        }
        EpothekeModule.set_cardlinkInteractionCB_onPhoneNumberRequest(onPhoneNumberRequestCB)

        let onSmsCodeRequestCB = () => {
          console.log(`onSmsCodeRequest`)
          EpothekeModule.setUserInput("123456")
          EpothekeModule.set_cardlinkInteractionCB_onSmsCodeRequest(onSmsCodeRequestCB)
        }
        EpothekeModule.set_cardlinkInteractionCB_onSmsCodeRequest(onSmsCodeRequestCB)

        /*
          Called if the sdk runs into an error.
        */
        let sdkErrorCB = (err: any, msg: any) => {
          console.log(`sdkError: ${msg}`)
          EpothekeModule.set_sdkErrorCB(sdkErrorCB)
        }
        EpothekeModule.set_sdkErrorCB(sdkErrorCB)


        /*
          Wiring of the controllerCallbacks
          These are not rewired, since they will only be called once.
        */
        //this callback informs about the start of the cardlink establishment
        EpothekeModule.set_controllerCallbackCB_onStarted(() => {
          console.log(`onStarted`)
        })

        /*
          This callback is called when the cardlink establishment is finished.

          If successfull the methods
            EpothekeModule.getReceipts()
            EpothekeModule.selectReceipts()
          become functional and can be called.
        */
        EpothekeModule.set_controllerCallbackCB_onAuthenticationCompletion(async (err: any, msg: any) => {
          console.log(`onAuthenticationCompletion error: ${err}`)
          console.log(`onAuthenticationCompletion protos: ${msg}`)

          try {
            //get available receipts
            let availReceipts = await EpothekeModule.getReceipts();
            console.log(`receipts: ${availReceipts}`)

            //example for a selection
            //which has to be done via a jsonstring containing the selectedPrescriptionList
            let confirmation = await EpothekeModule.selectReceipts(` {
              "type": "selectedPrescriptionList",
              "ICCSN": "PElDQ1NOPg",
              "medicationIndexList": [
                0,
                1,
                2
              ],
              "supplyOptionsType": "delivery",
              "messageId": "6f3b1c6b-334d-4378-aa4e-0bd61acaca08"
            }`);
            console.log(`selection confirmation: ${confirmation}`)

          } catch (e) {
            console.log(`error : ${e}`)
          }

        })


        //start the cardlink establishment
        EpothekeModule.startCardlink(`https://epotheke.mock.ecsec.services/cardlink?token=RANDOMTOKEN`)

      }} />
    </View>
  );
}
