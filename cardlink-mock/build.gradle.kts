import java.text.SimpleDateFormat
import java.util.*

plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    kotlin("plugin.serialization")
    //id("io.quarkus")
    alias(libs.plugins.quarkus)
}

val javaToolchain: String by project
//java.sourceCompatibility = JavaVersion.VERSION_1_8
kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(javaToolchain)
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
    annotation("io.quarkus.test.junit.QuarkusIntegrationTest")
}

quarkus {
    // set java toolchain executable for quarkus build commands as long as quarkus does not honor toolchains
    // https://github.com/quarkusio/quarkus/issues/20452
    buildForkOptions {
        val launcher = javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(javaToolchain)
        }
        val javaBin = launcher.map { it.executablePath.asFile.absolutePath }
        javaBin.orNull?.let { executable = it }
    }
}

dependencies {
    implementation(platform(libs.quarkus.bom))
    implementation(libs.bundles.quarkus.basics)
    implementation(libs.bundles.quarkusBuild)
    implementation(libs.quarkus.websockets)
    implementation(libs.quarkus.jackson)
    implementation(libs.kotlin.logging)
    implementation(libs.kotlin.jackson)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.quarkus.rest.client)
    implementation(libs.lib.phonenumber)

    testImplementation(libs.bundles.quarkus.basics.test)
    testImplementation(libs.quarkus.junit.mockito)
}
