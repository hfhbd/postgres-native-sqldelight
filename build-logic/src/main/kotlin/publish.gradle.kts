import org.gradle.api.artifacts.repositories.*
import org.gradle.api.publish.maven.*
import org.gradle.api.tasks.bundling.*
import org.gradle.kotlin.dsl.*
import java.util.*

plugins {
    `maven-publish`
    signing
}

val emptyJar by tasks.registering(Jar::class) { }

publishing {
    publications.configureEach {
        this as MavenPublication
        artifact(emptyJar) {
            classifier = "javadoc"
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
    useInMemoryPgpKeys(signingKey?.let { String(Base64.getDecoder().decode(it)).trim() }, signingPassword)
    sign(publishing.publications)
}
