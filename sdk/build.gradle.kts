description = "epotheke SDK Package"

plugins {
    id("epotheke.lib-jvm-conventions")
    id("epotheke.lib-android-conventions")
    id("epotheke.lib-ios-conventions")
    id("epotheke.publish-conventions")
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
            version = "2.1.11"
            ios.deploymentTarget = "13.0"
            moduleName = "OpenEcard"
        }
    }
}

android {
    namespace = "com.epotheke"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }

    packaging {
        resources.excludes.add("cif-repo/repo-config.properties")
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
