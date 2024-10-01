# Welcome to the react-native demo for epotheke-SDK integration

This app demonstrates how to integrate the epotheke-SDK in an app and can perform a happy path interaction with the
epotheke service.

## Some notes

- The app uses NFC which is not possible to test in emulators, hence make sure to use a real device with NFC switched on
- You will need a eGk to run through the prepared process (except for productive environment it has to be a test-eGk)

- When the status `requestCardInsertion` appears it is time to bring the eGK near the device.

- The test-service (mock) this app uses, does not send sms but accepts any TAN so (123456) is valid
- The phone number is not really needed, but must have a format of a valid german mobile number

## Start the app

With a console and directory of the app:

1. npm install
2. npm start

## Steps to follow for building apps

To make the SDK available for a custom app follow these steps:

### Android

#### Needed Repositories in ./android/app/build.gradle:

```groovy
repositories {
    google()
    mavenCentral()

    mavenLocal()
    maven {
        url = uri("https://mvn.ecsec.de/repository/openecard-public")
    }
    maven {
        url = uri("https://mvn.ecsec.de/repository/openecard-snapshot")
        mavenContent {
            snapshotsOnly()
        }
    }
}
```

#### Dependencies in ./android/app/build.gradle:

```groovy

def epothekeSdkVersion = "1.1.8"

dependencies {
    [...]
    implementation("com.epotheke:sdk:${epothekeSdkVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.+")

    // logging library, use whatever you like to output the slf4j log statements
    implementation("io.github.oshai:kotlin-logging:6.0.9")
    implementation("com.github.tony19:logback-android:3.0.0")
}

```

#### Packaging options in ./android/app/build.gradle:

You might need to add within the packaging options:

```groovy
resources {
    excludes += "/META-INF/{LICENSE.md,NOTICE.md,AL2.0,LGPL2.1}"
}
```

#### Permissions in ./android/app/src/main/AndroidManifest.xml:

Add the following permissions

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.NFC"/>
```

#### Logging

Copy the `./android/app/src/main/assetes/logback.xml` to the respective location in your app to enable logs in logcat.

#### Adding Native Module

Copy the following files to the respective folder in your app:

```
./android/app/src/main/java/de/ecsec/rn/epotheke/SdkModule.kt
./android/app/src/main/java/de/ecsec/rn/epotheke/SdkPackage.kt
```

Alter the `getPackages()` function in your `MainApplication.kt` to add the SdkPackage like shown below:

```kotlin
return PackageList(this).packages.apply {
    add(SdkPackage())
}
```

### iOS

#### Dependency in Podfile:
```
target '<CUSTOMAPPTARGET>' do

  [...]

  # Pod for epothekeDemo
  pod 'epotheke-sdk','>= 1.1.8'
```

#### Meta-files and permissions
Following entries have to exist in `Info.plist`:

```xml
<key>NFCReaderUsageDescription</key>
<string>NFC writer app</string>
[...]
<key>com.apple.developer.nfc.readersession.iso7816.select-identifiers</key>
<array>
    <string>D27600000102</string>
    <string>D2760001448000</string>
    <string>D27600014407</string>
    <string>D27600014408</string>
    <string>D2760001440A</string>
    <string>D2760001440B</string>
    <string>D2760001440C</string>
    <string>A000000167455349474E</string>
    <string>D27600006601</string>
</array>
[...]
```
There has to be a entitlements file with:
```xml
<key>com.apple.developer.nfc.readersession.formats</key>
<array>
    <string>TAG</string>
</array>
```

#### Adding Native Module
Copy the following files to the respective folder in your app:
```
./ios/RCTSdkModule.m
./ios/RCTSdkModule.h
```

### Usage in react native

Please refer to the example code in `App.tsx` on how to use the native module in react native.
