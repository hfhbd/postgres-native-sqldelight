import org.gradle.api.publish.maven.*
import org.gradle.api.tasks.bundling.*
import org.gradle.kotlin.dsl.*
import java.util.*

plugins {
    id("maven-publish")
    id("signing")
}

val emptyJar by tasks.registering(Jar::class) { }

publishing {
    publications.configureEach {
        this as MavenPublication
        if (project.name != "postgres-native-sqldelight-dialect") {
            artifact(emptyJar) {
                classifier = "javadoc"
            }
        }
        pom {
            name.set("app.softwork Postgres Native Driver and SqlDelight Dialect")
            description.set("A Postgres native driver including support for SqlDelight")
            url.set("https://github.com/hfhbd/SqlDelightNativePostgres")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("hfhbd")
                    name.set("Philip Wedemann")
                    email.set("mybztg+mavencentral@icloud.com")
                }
            }
            scm {
                connection.set("scm:git://github.com/hfhbd/SqlDelightNativePostgres.git")
                developerConnection.set("scm:git://github.com/hfhbd/SqlDelightNativePostgres.git")
                url.set("https://github.com/hfhbd/SqlDelightNativePostgres")
            }
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    signingKey?.let {
        useInMemoryPgpKeys(String(Base64.getDecoder().decode(it)).trim(), signingPassword)
        sign(publishing.publications)
    }
}

// https://youtrack.jetbrains.com/issue/KT-46466
val signingTasks = tasks.withType<Sign>()
tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(signingTasks)
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    filePermissions {}
    dirPermissions {}
}
