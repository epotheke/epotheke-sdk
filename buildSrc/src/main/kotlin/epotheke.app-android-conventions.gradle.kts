val androidCompileSdk: String by project
val androidMinSdk: String by project

plugins {
    id("epotheke.kotlin-conventions")
    id("com.android.application")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget {  }
}

android {
    compileSdk = androidCompileSdk.toInt()
    defaultConfig {
        minSdk = androidMinSdk.toInt()
    }
}
