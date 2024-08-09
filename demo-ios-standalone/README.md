# iOS-OeC-testApp 

This project provides a reference implementation and test app in swift for ios, showing the usage of the epotheke library.

---------------------

## Usage

### Prerequisite 
The epotheke sdk and the needed open-ecard sdk can be obtained via cocoapods. See: [Podfile](AlertController/Podfile) 

### Starting the demo  
1. Clone the project 
1. With command line navigate to AlertController and type 
    ```pod install```
1. Open iOS\_OeC\_testApp.xcworkspace in XCode
1. Start the app and have logs open
1. Type the button to start a cardlink based prescription order process with a epotheke test service


## Steps needed for building your own app

1. Configure your project for CocoaPods, if it is not allready the case 
1. Add the open-ecard and epotheke-sdk pods to your Podfile 
1. Make sure to have the NFC reader entitlement for "TAG" configured in your app setup
   (see AlertController/AlertController.entitlements file for reference) 
1. Make sure to have the AID for eGk smartcards listet in your Info.plist file
   (see following codeblock and AlertController/Info.plist file for reference) 
```xml
        <key>com.apple.developer.nfc.readersession.iso7816.select-identifiers</key>
        <array>
                <string>D27600000102</string>
        </array>
```

---------------------
