description = "eHealth SDK Package"

plugins {
    id("ehealth.lib-conventions")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                //put your multiplatform dependencies here
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.bundles.test.basics)
            }
        }
    }
}
