description = "epotheke SDK Package"

plugins {
    id("epotheke.kmp-lib-conventions")
    id("epotheke.kmp-jvm-lib-conventions")
    id("epotheke.kmp-android-lib-conventions")
    id("epotheke.kmp-ios-lib-conventions")
    id("epotheke.publish-conventions")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                //         implementation(libs.kotlin.stdlib)
                //         implementation(libs.kotlin.stdlib.common)
                implementation(libs.kotlin.logging)
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlin.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.websocket)
                implementation(libs.ktor.client.auth)

                implementation(libs.xmlutil.core)
                implementation(libs.xmlutil.ser)
                implementation(libs.fleeksoft.charset)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.bundles.test.basics.kotlin)
            }
        }
        val androidMain by getting {
            dependencies {
//                api(libs.oec.android)
                implementation(libs.ktor.client.okhttp)
            }
        }

        // TODO reactivate ios

        // val iosMain by getting {
        //     dependencies {
        //         implementation(libs.ktor.client.darwin)
        //     }
        // }
    }

    // TODO reactivate ios

    // cocoapods {
    //    name = "epotheke-sdk"
    //    homepage = "https://www.epotheke.com"
    //    summary = "iOS framework for integration of Epotheke services"
    //    authors = "florian.otto@ecsec.de"
    //    license = "GPLv3"
    //    framework {
    //        baseName = "epotheke"
    //        binaryOption("bundleId", "com.epotheke.sdk")
    //    }

    //    pod("open-ecard") {
    //        version = libs.versions.oec.get()
    //        ios.deploymentTarget = "13.0"
    //        moduleName = "OpenEcard"
    //    }
    // }
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
