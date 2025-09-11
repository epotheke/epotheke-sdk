//
//  ContentView.swift
//  EpothekeDemo
//
//  Created by Florian Otto on 13.08.24.
//

import SwiftUI
import epotheke

extension NSError {
    var kotlinError: Any? {
        guard domain == "KotlinException" else { return nil }
        return userInfo["KotlinException"]
    }
}
struct ContentView: View {
    @State var showInput = false
    @State var inputTitle = "Enter phone number"
    @State var inputMsg = "Enter phone number"
    @State var input = ""

    @State var status = ""

    @State var epotheke: Epotheke?

    init() {
        IosNfcAlertMessages().cardConnectedMessage = "Card connected."
        IosNfcAlertMessages().cardInsertedMessage = "Card inserted."
        IosNfcAlertMessages().cardNotSupported = "Card not supported."
        IosNfcAlertMessages().provideCardMessage = "Please bring eGK to device."
    }

    @State var continuation: CheckedContinuation<String, Never>?

    var body: some View {
        VStack(spacing: 32) {
            Button {
                print("Starting epotheke test case")
                performEpo(
                    url: "https://mock.test.epotheke.com/cardlink"
                )
            } label: {
                Text("Establish Cardlink (Mock)")
            }
            Button {
                print("Starting epotheke test case")
                performEpo(
                    url: "https://service.dev.epotheke.com/cardlink"
                )
            } label: {
                Text("Establish Cardlink (DEV)")
            }
            Button {
                print("Starting epotheke test case")
                performEpo(
                    url: "https://service.staging.epotheke.com/cardlink",
                    tenantToken:
                        "eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiMDE5MmRlMjktMjZhMi03MDAwLTkyMjAtMGFlMDU4YWY2NjE0IiwiaWF0IjoxNzMwMzA0MTE0LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTc5MzM3NjExNCwianRpIjoiNGFjMjExN2MtZWVmMC00ZGU1LWI0YTAtMDQ0YjEwMGViNDM3In0.ApEv-ThtB1Z3UbXZoRDpP5YPIM3kIqGGat5qXwPGxhsvT-w5lokaca4w3G_8lmTgZ_FSXCksudOCXhTf2bw6wA"
                )
            } label: {
                Text("Establish Cardlink (Staging)")
            }
            Button {
                print("Starting epotheke test case")
                performEpo(
                    url: "https://service.epotheke.com/cardlink",
                    tenantToken:
                        "eyJraWQiOiJ0ZW5hbnQtc2lnbmVyLTIwMjQxMTA2IiwiYWxnIjoiRVMyNTYiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiMDE5M2NlZTMtMTdkOC03MDAwLTkwOTktZmM4NGNlMjYyNzk1IiwiaWF0IjoxNzQxMTczNzM4LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTgwNDI0NTczOCwianRpIjoiYWE2NDA5NWMtY2NlNy00N2FjLWEzZDItYzA2ZThlYjE2MmVmIn0.L0D7XGchxtkv_rzvzvru6t80MJy8aQKhbiTReH69MNBVgp9Z-wUlDgIPdpbySmhDSTVEbp1rCwQAOyXje1dntQ"
                )
            } label: {
                Text("Establish Cardlink (Prod)")
            }
            Button {
                print("Starting fetch")
                fetchPrescriptions()
            } label: {
                Text("Fetch Prescriptions")
            }
            Text("Status:")
            TextField("status", text: $status).alert(Text("UserInput"), isPresented: $showInput) {
                Button("OK") {
                    showInput = false
                    continuation?.resume(returning: input)
                    continuation = nil
                }
                TextField("InField", text: $input)
            } message: {
                Text(inputMsg)
            }

        }
    }

    /* This class implements the interactions with the sdk during the cardlink process
     When for example the CAN of the card is needed, the sdk will call the appropriate function.
     Within the app the user can get asked for the CAN.
     */

    @MainActor
    class CardLinkInteraction: NSObject, UserInteraction {

        var v: ContentView

        init(v: ContentView) {
            self.v = v
            super.init()
        }

        func onPhoneNumberRequest() async throws -> String {
            await withCheckedContinuation { continuation in
                self.v.showInput = false
                self.v.inputTitle = "Number"
                self.v.inputMsg = "Enter number"
                self.v.input = "+49 151 123 123 23"
                self.v.showInput = true

                self.v.continuation = continuation
            }
        }

        func onPhoneNumberRetry(resultCode: ResultCode, msg errorMessage: String?) async throws -> String {
            await withCheckedContinuation { continuation in
                self.v.showInput = false
                self.v.inputTitle = "Number"
                self.v.inputMsg = "Problem with number: \(errorMessage ?? "")"
                self.v.input = "+49 151 123 123 23"
                self.v.showInput = true
                self.v.status = "Problem with number: \(errorMessage ?? "")"

                self.v.continuation = continuation
            }
        }
        func onTanRequest() async throws -> String {
            await withCheckedContinuation { continuation in
                self.v.showInput = false
                self.v.inputTitle = "TAN"
                self.v.inputMsg = "Enter TAN"
                self.v.input = "123123"
                self.v.showInput = true

                self.v.continuation = continuation
            }
        }

        func onTanRetry(resultCode: ResultCode, msg errorMessage: String?) async throws -> String {
            await withCheckedContinuation { continuation in
                self.v.showInput = false
                self.v.inputTitle = "TAN"
                self.v.inputMsg = "Problem with TAN: \(errorMessage ?? "")"
                self.v.input = "123123"
                self.v.showInput = true
                self.v.status = "Problem with TAN: \(errorMessage ?? "")"

                self.v.continuation = continuation
            }
        }
        func onCanRequest() async throws -> String {
            await withCheckedContinuation { continuation in
                self.v.showInput = false
                self.v.inputTitle = "CAN"
                self.v.inputMsg = "Enter CAN"
                self.v.input = "123123"
                self.v.showInput = true

                self.v.continuation = continuation
            }
        }
        func onCanRetry(resultCode: CardCommunicationResultCode) async throws -> String {
            await withCheckedContinuation { continuation in
                self.v.showInput = false
                self.v.inputTitle = "CAN"
                self.v.inputMsg = "CAN was wrong: \(resultCode.msg ?? "")"
                self.v.input = "123123"
                self.v.showInput = true
                self.v.status = "CAN was wrong: \(resultCode.msg ?? "")"

                self.v.continuation = continuation
            }
        }

        func requestCardInsertion() async throws {
            print("requestCardInsertion")
        }

        func onCardRecognized() async throws {
            print("onCardRecognized")
        }

    }

    func performEpo(
        url: String,
        tenantToken: String? = nil
    ) {

        let terminalFactory = IosTerminalFactory.companion.instance
        let epo = Epotheke(
            terminalFactory: terminalFactory, serviceUrl: url, tenantToken: tenantToken, wsSessionId: nil)
        let authProt = epo.cardLinkAuthenticationProtocol
        CardLinkAuthenticationConfig().readPersonalData = true
        CardLinkAuthenticationConfig().readInsurerData = false

        Task {
            do {
                let authResult = try await authProt.establishCardLink(interaction: CardLinkInteraction(v: self))
                epotheke = epo
                status =
                    "Cardlink established for \(authResult.personalData?.versicherter?.person?.vorname ?? "vn") \(authResult.personalData?.versicherter?.person?.nachname ?? "nn")"
            } catch let error as NSError {

                switch error.kotlinError {
                case let e as CardLinkClientError:
                    status = e.message
                case let e as CardLinkError:
                    status = e.message
                case .some(let e):
                    status = (e as? KotlinException)?.message ?? "missing error message"
                case .none:
                    status = "error in establish cardlink with unmapped error"
                }

            }
        }
    }
    @MainActor
    func fetchPrescriptions() {
        guard let prescProt = epotheke?.prescriptionProtocol else {
            status = "Cardlink not established."
            return
        }

        Task {
            do {
                let prescriptions = try await prescProt.requestPrescriptions(
                    req: RequestPrescriptionList(iccsns: [], messageId: "")
                )
                let nbrForFirstCard = prescriptions.availablePrescriptionLists.first?.prescriptionBundleList.count ?? 0
                status = "Found \(nbrForFirstCard) prescriptions for card."
            } catch let error as NSError {

                switch error.kotlinError {
                case let e as PrescriptionProtocolException:
                    status = e.genericErrorMessage.errorMessage
                case .some(let e):
                    status = (e as? KotlinException)?.message ?? "missing error message"
                case .none:
                    status = "error in establish cardlink with unmapped error"
                }

            }

        }

    }

}

#Preview {
    ContentView()
}
