plugins {
    io.github.`gradle-nexus`.`publish-plugin`
    org.jetbrains.dokka
}

tasks.dokkaHtmlMultiModule {
    includes.from("README.md")
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}
