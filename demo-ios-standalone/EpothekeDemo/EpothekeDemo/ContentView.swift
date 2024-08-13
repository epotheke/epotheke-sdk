//
//  ContentView.swift
//  EpothekeDemo
//
//  Created by Florian Otto on 13.08.24.
//

import SwiftUI
import CoreNFC
import OpenEcard.open_ecard_mobile_lib
import Epotheke

struct ContentView: View {
    var body: some View {
        VStack {
            Button {
                print("Starting epotheke test case")
                performEpo()
            } label: {
                Text("Epotheke Demo")
            }
        }
        .padding()
    }

    /* This implementation allows to configure messages which are shown by iOS during nfc interaction*/
    class IOSNFCOptions: NSObject, NFCConfigProtocol {
        func getProvideCardMessage() -> String! {
            return "Please hold card to your phone"
        }

        func getDefaultNFCCardRecognizedMessage() -> String! {
            return "Please wait. A card has been detected"
        }

        func getDefaultNFCErrorMessage() -> String! {
            return "An error occurred communicating with the card."
        }

        func getAquireNFCTagTimeoutMessage() -> String! {
            return "Could not connect to a card. Please try again."
        }

        func getNFCCompletionMessage() -> String! {
            return "Finished communicating with the card"
        }

        func getTagLostErrorMessage() -> String! {
            return "Contact was lost with the card"
        }

        func getDefaultCardConnectedMessage() -> String! {
            return "Connected with the card."
        }


    }

    /* This implementation will handle errors whcih might occur within sdk or during processes the sdk handles.*/
    class SdkErrorHandlerImp :NSObject, SdkErrorHandler {
        func hdl(error: NSObject?) {
            if let e = error {
                print((e as! any ServiceErrorResponseProtocol).getErrorMessage())
            } else  {
                print("error")
            }
        }
    }

    /* This class implements the interactions with the sdk during the cardlink process
     When for example the CAN of the card is needed, the sdk will call the appropriate function.
     Within the app the user can get asked for the CAN.
     After that the handed in callback handler has to be called with the given value to resume the process.
     */
    class CardLinkInteraction: NSObject,  CardLinkInteractionProtocol{
        //var v :ViewController

        //init(v:ViewController) {
        //    self.v = v
        //    super.init()
        //}


        func onCanRequest(_ enterCan: (NSObjectProtocol & ConfirmPasswordOperationProtocol)!) {
            print("onCanRequest")
            enterCan.confirmPassword("753031")
        }

        func onPhoneNumberRequest(_ enterPhoneNumber: (NSObjectProtocol & ConfirmTextOperationProtocol)!) {
            print("onPhoneNumberRequest")
            enterPhoneNumber.confirmText("+4915123456789")
            print("onPhoneNumberRequest")
        }

        func onSmsCodeRequest(_ smsCode: (NSObjectProtocol & ConfirmPasswordOperationProtocol)!) {
            print("onSmsCodeRequest")
            smsCode.confirmPassword("123123")
        }

        func requestCardInsertion() {
            print("requestCardInsertion")
        }

        func requestCardInsertion(_ msgHandler: (NSObjectProtocol & NFCOverlayMessageHandlerProtocol)!) {
            print("requestCardInsertion")
        }

        func onCardInteractionComplete() {
            print("onCardInteractionComplete")
        }

        func onCardRecognized() {
            print("onCardRecognized")
        }

        func onCardRemoved() {
            print("onCardRemoved")
        }

    }

    /* This class implements the CardlinkController interface.
     After the cardlink establishment the function "onAuthenticationCompletion" will be called with the result of the cardlink process

     If the establishment was successfull (ActivationResultCode is OK) the given cardLinkProtocols-Set
     will contain working objects to work with the established connection.
     Currently the first and only object is an implementation of the PrescriptionProtocol, which allows the request available
     prescriptions and to send back a selection for purchasing them.
     */
    class CardLinkController: NSObject, CardLinkControllerCallback {
        func onAuthenticationCompletion(p0: ActivationResultProtocol?, cardLinkProtocols: Set<AnyHashable>) {
            print("onAuthComp")
            if(p0?.getCode() == ActivationResultCode.OK) {
                let p = cardLinkProtocols.first
                let p1 = p as! any PrescriptionProtocol

                let req = RequestPrescriptionList(iccsns: [KotlinByteArray(size: 0)], messageId: RandomUUID_iosKt.randomUUID())

                DispatchQueue.main.sync {
                    p1.requestPrescriptions(req: req) { response,er in
                        if let iccsn = response?.availablePrescriptionLists.first?.iccsn {
                            print(iccsn)
                            let selectAll = SelectedPrescriptionList(
                                iccsn: iccsn,
                                prescriptionIndexList: [""],
                                version: nil,
                                supplyOptionsType: SupplyOptionsType.delivery,
                                name: nil,
                                address: nil,
                                hint: nil,
                                text: nil,
                                phone: nil,
                                mail: nil,
                                messageId: "id"
                            )
                            DispatchQueue.main.sync {
                                p1.selectPrescriptions(selection: selectAll) { responseSel, err in
                                    print(responseSel!)
                                }
                            }
                        }
                    }
                }
            } else {
                print("process ended with an error.")
            }


        }

        /* Called when the cardlink process is started */
        func onStarted() {
            print("onStarted")
        }

    }


//    lazy var sdk : SdkCore? = nil

    /*This function initialises the above implementations and starts the epotheke prescription process*/
    func performEpo() {
        let cardLinkController = CardLinkController()
        let sdkErrorHandler = SdkErrorHandlerImp()
        let cardLinkInteraction = CardLinkInteraction()
        let url = "https://epotheke.mock.ecsec.services/cardlink?token="+RandomUUID_iosKt.randomUUID()
        let sdk = SdkCore(cardLinkUrl: url,
                      cardLinkControllerCallback: cardLinkController,
                      cardLinkInteractionProtocol: cardLinkInteraction,
                      sdkErrorHandler: sdkErrorHandler,
                      nfcOpts: IOSNFCOptions())
        sdk.doInitCardLink()

    }


}

#Preview {
    ContentView()
}
