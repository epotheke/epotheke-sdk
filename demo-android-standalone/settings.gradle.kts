pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        mavenLocal()
        maven {
            url = uri("https://mvn.ecsec.de/repository/openecard-public")
        }
    }
}

rootProject.name = "epotheke SDK Demo App"
include(":app")
