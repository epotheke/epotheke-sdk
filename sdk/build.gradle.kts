description = "epotheke SDK Package"

plugins {
    id("epotheke.kmp-lib-conventions")
    // id("epotheke.kmp-jvm-lib-conventions")
    id("epotheke.kmp-android-lib-conventions")
    id("epotheke.kmp-ios-lib-conventions")
    id("epotheke.publish-conventions")
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

            implementation(libs.xmlutil.core)
            implementation(libs.xmlutil.ser)
            implementation(libs.fleeksoft.charset)
            implementation(libs.bundles.oec.cardlink)

            implementation(libs.okio)
        }

        commonTest.dependencies {
            implementation(libs.bundles.test.basics.kotlin)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            api(libs.oec.smartcard.pcsc.android)
        }

        androidHostTest.dependencies { }
        androidDeviceTest.dependencies {
            implementation(libs.bundles.test.android.kotlin)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            api(libs.oec.smartcard.pcsc.ios)
        }
    }

    cocoapods {
        name = "epotheke-sdk"
        homepage = "https://www.epotheke.com"
        summary = "iOS framework for integration of Epotheke services"
        authors = "florian.otto@ecsec.de"
        license = "GPLv3"
        framework {
            baseName = "epotheke"
            export(libs.oec.smartcard.pcsc.ios)
            binaryOption("bundleId", "com.epotheke.sdk")
        }
    }
}

// TODO needed?

// android {
//    namespace = "com.epotheke"
//
//    packaging {
//        resources.excludes.add("cif-repo/repo-config.properties")
//    }
//
//    buildTypes {
//        defaultConfig {
//            consumerProguardFiles("./consumer-proguard.txt")
//        }
//    }
// }

// TODO needed?

// publishing {
//    publications {
//        register<MavenPublication>("release") {
//            afterEvaluate {
//                from(components["release"])
//            }
//        }
//    }
// }
