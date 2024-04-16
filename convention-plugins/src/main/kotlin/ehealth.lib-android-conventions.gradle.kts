import gradle.kotlin.dsl.accessors._e17afd335abbdd9764f13315a15fdf50.kotlin
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.assign

val androidCompileSdk: String by project
val androidMinSdk: String by project

plugins {
    id("ehealth.lib-conventions")
    id("com.android.library")
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
