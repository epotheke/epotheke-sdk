val androidCompileSdk: String by project
val androidMinSdk: String by project

plugins {
    id("epotheke.kotlin-conventions")
    id("com.android.application")
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
