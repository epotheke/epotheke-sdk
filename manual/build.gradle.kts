import de.ecsec.PublishNexusRawTask

plugins {
    alias(libs.plugins.asciidoctor)
    alias(libs.plugins.rawPublish)
}

asciidoctorj {
    fatalWarnings(missingIncludes())
    modules {
        diagram { use() }
    }
}

tasks.asciidoctor {
    baseDirFollowsSourceDir()
    sources {
        include("index.adoc")
    }
    attributes(mapOf(
        "toc" to "left",
        "toclevels" to "4",
        "sectnums" to "",
        "sectanchors" to "",
        "source-highlighter" to "rouge",
    ))
}


publishNexusRaw {
    nexusUrl = "https://mvn.ecsec.de/"
    repoName = "data-public"
    username = System.getenv("MVN_ECSEC_USERNAME") ?: project.findProperty("mvnUsernameEcsec") as String?
    password = System.getenv("MVN_ECSEC_PASSWORD") ?: project.findProperty("mvnPasswordEcsec") as String?
    inputDir = layout.buildDirectory.dir("docs/asciidoc")
}

tasks.register("publishNexusRawVersion", PublishNexusRawTask::class) {
    group = "publishing"
    repoFolder = "epotheke/sdk/doc/${project.version}"
    dependsOn("asciidoctor")
}
tasks.register("publishNexusRawLatest", PublishNexusRawTask::class) {
    group = "publishing"
    repoFolder = "epotheke/sdk/doc/latest"
    dependsOn("asciidoctor")
}
tasks.register("publishNexusRawDev", PublishNexusRawTask::class) {
    group = "publishing"
    repoFolder = "epotheke/sdk/doc/dev"
    dependsOn("asciidoctor")
}
