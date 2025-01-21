description = "epotheke SDK Package"

plugins {
    //id("epotheke.lib-jvm-conventions")
    id("epotheke.lib-android-conventions")
    id("epotheke.lib-ios-conventions")
    id("epotheke.publish-conventions")

    kotlin("plugin.serialization")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.logging)
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlin.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.websocket)
                implementation(libs.ktor.client.auth)
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
                implementation(libs.ktor.client.okhttp)
            }
        }
        val iosMain by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
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
            binaryOption("bundleId", "com.epotheke.sdk")
        }

        pod("open-ecard") {

            // TODO: use version from catalogue
//            version = libs.versions.oec.get()
            source = path("/Users/florianotto/pod")
            ios.deploymentTarget = "13.0"
            moduleName = "OpenEcard"
        }
    }
}

android {
    namespace = "com.epotheke"

    packaging {
        resources.excludes.add("cif-repo/repo-config.properties")
    }

    buildTypes {
        defaultConfig {
            consumerProguardFiles("./consumer-proguard.txt")
        }
    }

    publishing {
        singleVariant("release") {
            // if you don't want sources/javadoc, remove these lines
            withSourcesJar()
            withJavadocJar()
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
