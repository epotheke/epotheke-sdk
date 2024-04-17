description = "epotheke SDK Package"

plugins {
    id("epotheke.lib-jvm-conventions")
    id("epotheke.lib-android-conventions")
    id("epotheke.lib-ios-conventions")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.logging)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.bundles.test.basics)
            }
        }
        val androidMain by getting {
            dependencies {
                api(libs.oec.android)
            }
        }
    }

    cocoapods {
        pod("open-ecard") {
            // TODO: use version from catalogue
            version = "2.1.6"
        }
    }
}

android {
    namespace = "de.epotheke"
}
