plugins {
    id("epotheke.kmp-conventions")
    kotlin("native.cocoapods")
}

kotlin {
    iosArm64 { }
    iosSimulatorArm64 { }
}
