plugins {
    id("epotheke.kmp-conventions")
    // todo do we need this
    // kotlin("native.cocoapods")
}

kotlin {
    iosArm64 { }
    iosSimulatorArm64 { }
}
