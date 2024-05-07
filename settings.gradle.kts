pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
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

rootProject.name = "epotheke-sdk"

include("sdk")
include("demo-app")

include("cardlink-mock")
