import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinAllOpen)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.quarkus)
}

kotlin {
    compilerOptions.jvmTarget = JvmTarget.JVM_21
}

java {
    targetCompatibility = JavaVersion.VERSION_21
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
    annotation("io.quarkus.test.junit.QuarkusIntegrationTest")
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
