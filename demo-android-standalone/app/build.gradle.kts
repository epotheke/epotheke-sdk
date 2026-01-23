plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.epotheke.demo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.epotheke.demo"
        minSdk = 23
        targetSdk = 36
        versionCode = 12
        versionName = "1.0.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        multiDexEnabled = true
    }
    android {
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }

    kotlin {
        jvmToolchain(21)
    }

    buildTypes {
        release {
            // shrinkResources true
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{LICENSE.md,NOTICE.md,AL2.0,LGPL2.1}"
        }
    }
}

val epothekeSdkVersion = "2.0.0"

dependencies {
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")

    // the actual epotheke library
    implementation("com.epotheke:sdk:$epothekeSdkVersion") {
        exclude(group = "io.github.oshai", module = "kotlin-logging-android-debug")
    }

    // logging
    implementation("io.github.oshai:kotlin-logging-android:7.0.14")
    implementation("com.github.tony19:logback-android:3.0.0")
}
