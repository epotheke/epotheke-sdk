pluginManagement {
    repositories {
        gradlePluginPortal()
        google()

        maven {
            url = uri("https://mvn.ecsec.de/repository/openecard-public")
        }
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

include("manual")

include("sdk")
// include("demo-app")

includeBuild("cardlink-mock")
