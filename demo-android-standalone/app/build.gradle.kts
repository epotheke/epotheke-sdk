plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.epotheke.demo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.epotheke.demo"
        minSdk = 21
        targetSdk = 34
        versionCode = 12
        versionName = "1.0.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
//        multiDexEnabled = true
    }

    buildTypes {
        release {
            //shrinkResources true
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // make sure the dex compiler translates all java 8 constructs to be compatible with older APIs
    // https://developer.android.com/studio/write/java8-support
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{LICENSE.md,NOTICE.md,AL2.0,LGPL2.1}"
        }
    }
}

val epothekeSdkVersion = "1.1.5-SNAPSHOT"

dependencies {
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // the actual epotheke library
    implementation("com.epotheke:sdk:${epothekeSdkVersion}")

    // logging library, use whatever you like to output the slf4j log statements
    implementation("io.github.oshai:kotlin-logging:6.0.9")
    implementation("com.github.tony19:logback-android:3.0.0")

}
