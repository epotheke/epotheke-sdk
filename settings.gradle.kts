pluginManagement {
    includeBuild("convention-plugins")
    repositories {
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://mvn.ecsec.de/repository/openecard-public")
        }
    }

    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}

rootProject.name = "ehealth-sdk"

include("sdk")
include("demo-app")
