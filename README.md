# epotheke-SDK

The epotheke SDK is a Kotlin Multi-Platform (KMP) library implementing the client side of the [CardLink](https://www.epotheke.com/#eH-CL) protocol.
It takes care of the communication with the CardLink service and the handling of the eHealth card.

Demo applications for native android and ios apps can be found in folders: 
- [demo-android-standalone](demo-android-standalone)
- [demo-ios-standalone](demo-ios-standalone)

The latest documentation can be found at https://mvn.ecsec.de/repository/data-public/epotheke/sdk/doc/latest/.

## App Integration

The epotheke SDK provides an API which can be integrated directly using the `Epotheke` class.

`Epotheke` provides a factory method `createEpothekeService` which needs as parameters:
- terminalFactory: for NFC functionality 
- serviceUrl: the url of the epotheke service
- tenantToken: An access token
- wsSessionId: Optional which can be used to reconnect to an existing session.
- cifs: Optional which allows to change CardDefinition configurations, defaults to eGK
- recognition: Optional which allows to control if card recognition is used, default switches off recognition.


`AndroidTerminaFactory` can be gathered by calling `AndroidTerminalFactory.instance(activity)` providing a reference to the android activity.
Note that the `onNewIntent` method must be overridden by the implementing app, and `Intents` have to be provided to the `AndroidTerminalFactory` which is needed for NFC communication.
`IosTerminalFactory` by IosTerminalFactory.companion.instance.
For mobile cases there is a default implementation `SmartCardConnector` that can be instantiated with such a `TerminalFactory` and used directly.

If `recognition` is switched off, each detected card is assumed to be an eGK card.
If it is another card, this will lead to errors later in the process.
This however shortens the process and the needed messages, which are exchanged via NFC.


An instance of `Epotheke` class provides instances of: 
- `CardlinkAuthenticationProtocol`
- `PrescriptionProtocol`
 
which can be used to perform different functions.
 
`CardlinkAuthenticationProtocol` allows to call `establishCardlink(interaction)` which performs an authentication process 
with epotheke service using SMS/Tan verification and an eGK card. `interaction` has to be an implementation of `CardlinkInteraction` interface.
An instance of `CardlinkInteraction` enables the protocol to get needed data and inform calling code about current steps.
In addition, it has to provide a `SmartCardDeviceConnection` when the process needs to communicate with the eGK.
To establish a `SmartCardDeviceConnection` an instance of `SmartCardSalSession` is provided and can be used.

`PrescriptionProtocol` allows to call `requestPrescriptions()` which fetches available Prescriptions for registered eGK cards in the session.

For details on how to configure your app project, refer to the [manual](https://mvn.ecsec.de/repository/data-public/epotheke/sdk/doc/latest/).

![epotheke SDK Interfaces](manual/src/docs/asciidoc/img/SDK_interfaces.svg "epotheke SDK Interfaces")


## Building the SDK
```bash
./gradlew build
```

In order to build the SDK for use in an app, it can be published to the local maven repository with the following command:
```bash
./gradlew publishToMavenLocal
```
After successful execution of the command, the SDK can be used as shown in the demo application.

## License

The epotheke SDK uses a Dual Licensing model.
The software is always distributed under the GNU General Public License v3 (GPLv3).
Additionally, the software can be licensed in an individual agreement between the licenser and the licensee.
