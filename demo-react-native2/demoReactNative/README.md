# Welcome to the expo based demo for epotheke sdk integration (currently only android)

## Start the app 
1. npm install
2. npm run android

## Steps to follow for other apps
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

def EpothekeSdkVersion = "1.1.0-rc.1"

dependencies {
     ... 
    implementation("com.epotheke:sdk:${EpothekeSdkVersion}")
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
./android/app/src/main/java/de/ecsec/rn/epotheke/EpothekeModule.kt
./android/app/src/main/java/de/ecsec/rn/epotheke/EpothekePackage.kt
```

Alter the `getPackages()` function in your `MainApplication.kt` to add the EpothekePackage like shown below:

```kotlin
return PackageList(this).packages.apply{
    add(EpothekePackage())
}
```

### Usage in react native

Please refer to the example code in `/app/index.tsx` on how to use the native module in react native.
