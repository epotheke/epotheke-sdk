description = "epotheke SDK Package"

plugins {
    id("epotheke.kmp-lib-conventions")
    // id("epotheke.kmp-jvm-lib-conventions")
    id("epotheke.kmp-android-lib-conventions")
    id("epotheke.kmp-ios-lib-conventions")
    id("epotheke.cocoapods-conventions")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.logging)

            implementation(libs.kotlin.coroutines.core)
            implementation(libs.kotlin.serialization.json)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.websocket)
            implementation(libs.ktor.client.auth)

            api(project(":egk-core"))
            api(project(":egk-data"))
            // used directly in Epotheke class, so transitive implementation dependency is not enough
            implementation(libs.oec.bundled.cifs)
            implementation(libs.oec.pace)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(libs.bundles.test.basics.kotlin)
        }

        androidHostTest.dependencies { }
        androidDeviceTest.dependencies {
            implementation(libs.bundles.test.android.kotlin)
        }
    }

    cocoapods {
        name = "epotheke-sdk"
        homepage = "https://www.epotheke.com"
        summary = "iOS framework for integration of Epotheke services"
        authors = "florian.otto@ecsec.de"
        license = "GPLv3"
        ios.deploymentTarget = "12.0"
        framework {
            baseName = "epotheke"
            export(libs.oec.smartcard.pcsc.ios)
            export(libs.oec.smartcard.sal)
            binaryOption("bundleId", "com.epotheke.sdk")
        }
    }
}
