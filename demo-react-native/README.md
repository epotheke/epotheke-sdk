# Welcome to the demo for epotheke sdk integration (currently only android)

This app demonstrates how to integrate the epotheke-SDK in an app and can perform a happy path interaction with the epotheke service.

## Some notes
- The app uses NFC which is not possible to test in emulators, hence make sure to use a real device with NFC switched on
- You will need a eGk to run through the prepared process
- You will have to adjust App.tsx to provide the fitting CAN for this very card to the SDK
- You won't have any UI except for the button to start the showcase
- Everything else is in the console.log

- When the log `requestCardInsertion` appears it is time to bring the eGK near the device. 
- In the end there should be a log containing testdata

- The test-service this app uses does not send sms but accepts any TAN so (123456) is valid
- The phonenumber is not needed, too but must have a format of a valid german mobile number
 
## Start the app  
1. npm install
2. npm start
3. type a when up 

## Steps to follow for building apps
To make the sdk available for a custom app follow these steps:  

### Repositories in ./android/app/build.gradle: 

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

### Dependencies in ./android/app/build.gradle: 

```groovy 

def epothekeSdkVersion = "1.1.0-rc.1"

dependencies {
     ... 
    implementation("com.epotheke:sdk:${epothekeSdkVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.+")
    
    // logging library, use whatever you like to output the slf4j log statements
    implementation("io.github.oshai:kotlin-logging:6.0.9")
    implementation("com.github.tony19:logback-android:3.0.0")
}

```

### Packaging options in ./android/app/build.gradle:
You might need to add within the packaging options:
```groovy

        resources {
            excludes += "/META-INF/{LICENSE.md,NOTICE.md,AL2.0,LGPL2.1}"
        }
```

### Permissions in ./android/app/src/main/AndroidManifest.xml:

Add the following permissions
```xml
  <uses-permission android:name="android.permission.INTERNET" />
  <uses-permission android:name="android.permission.NFC" />
```

### Logging
Copy the `./android/app/src/main/assetes/logback.xml` to the respective location in your app to enable logs in logcat.

### Adding Native Module
Copy the following files to the respective folder in your app: 

```
./android/app/src/main/java/de/ecsec/rn/epotheke/SdkModule.kt
./android/app/src/main/java/de/ecsec/rn/epotheke/SdkPackage.kt
```

Alter the `getPackages()` function in your `MainApplication.kt` to add the SdkPackage like shown below:

```kotlin
return PackageList(this).packages.apply{
    add(SdkPackage())
}
```

### Usage in react native

Please refer to the example code in `App.tsx` on how to use the native module in react native.
