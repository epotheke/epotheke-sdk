description = "epotheke SDK eGK Data-Module"

plugins {
    id("epotheke.kmp-lib-conventions")
    id("epotheke.kmp-android-lib-conventions")
    id("epotheke.kmp-ios-lib-conventions")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.logging)

            implementation(libs.kotlin.serialization.json)
            implementation(libs.xmlutil.core)
            implementation(libs.xmlutil.ser)

            implementation(libs.okio)
            implementation(libs.fleeksoft.charset)

            api(project(":egk-core"))
        }

        androidMain.dependencies {
        }

        iosMain.dependencies {
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
