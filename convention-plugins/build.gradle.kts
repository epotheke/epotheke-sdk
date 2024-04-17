plugins {
    // Support convention plugins written in Kotlin. Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
    `kotlin-dsl`
}


dependencies {
    implementation(libs.plugins.kotlinMultiplatform)
    implementation(libs.plugins.kotlinCocoapods)
    implementation(libs.plugins.androidLibrary)
    implementation(libs.plugins.androidApplication)
    implementation(libs.plugins.jetbrainsCompose)
}

fun DependencyHandlerScope.implementation(pluginProv: Provider<PluginDependency>) {
    pluginProv.orNull ?.let { plugin ->
        val pluginId = plugin.pluginId
        val pluginVer = plugin.version
        this.implementation("$pluginId:$pluginId.gradle.plugin") {
            version {
                branch = pluginVer.branch
                prefer(pluginVer.preferredVersion)
                if (pluginVer.requiredVersion.isNotBlank()) {
                    require(pluginVer.requiredVersion)
                } else {
                    strictly(pluginVer.strictVersion)
                }
            }
        }
    }
}
