import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("multiplatform")
}

val javaToolchain: String by project
kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(javaToolchain)
    }
    //java.targetCompatibility = JavaVersion.valueOf("11")

    applyDefaultHierarchyTemplate()

    compilerOptions {
        this.languageVersion = KotlinVersion.KOTLIN_2_0
    }
}

val testHeapSize: String by project
tasks.withType<Test> {
    maxHeapSize = testHeapSize
    useJUnitPlatform {  }
}
