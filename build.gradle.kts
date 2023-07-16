plugins {
    id("io.github.gradle-nexus.publish-plugin")
    id("org.jetbrains.dokka")
}

tasks.dokkaHtmlMultiModule {
    includes.from("README.md")
}

nexusPublishing {
    this.repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}
