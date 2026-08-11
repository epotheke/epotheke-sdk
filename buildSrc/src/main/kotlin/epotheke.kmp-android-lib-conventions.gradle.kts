val androidCompileSdk = project.property("androidCompileSdk") as String
val androidMinSdk = project.property("androidMinSdk") as String

plugins {
    id("epotheke.kmp-conventions")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    android {
        namespace = "${project.group}.${project.name}".replace("-", "_")

        minSdk = androidMinSdk.toInt()
        compileSdk = androidCompileSdk.toInt()

        withHostTestBuilder { }
        withDeviceTestBuilder { }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            packaging {
                resources.excludes.add("META-INF/*")
            }
        }
    }
}
