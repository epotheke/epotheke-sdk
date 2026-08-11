description = "epotheke SDK eGK Core-Module"

plugins {
    id("epotheke.kmp-lib-conventions")
    id("epotheke.kmp-android-lib-conventions")
    id("epotheke.kmp-ios-lib-conventions")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.logging)

            implementation(libs.bundles.oec.cardlink)
            api(libs.oec.smartcard.sal)
        }

        androidMain.dependencies {
            api(libs.oec.smartcard.pcsc.android)
        }

        iosMain.dependencies {
            api(libs.oec.smartcard.pcsc.ios)
        }

        commonTest.dependencies {
            implementation(libs.bundles.test.basics.kotlin)
        }

        androidHostTest.dependencies { }
        androidDeviceTest.dependencies {
            implementation(libs.bundles.test.android.kotlin)
        }
    }
}
