import app.cash.licensee.*
import org.jetbrains.kotlin.gradle.dsl.*
import java.util.*

plugins {
    kotlin("multiplatform") version "1.7.20" apply false
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.11.1"
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
    id("app.cash.sqldelight") version "2.0.0-alpha04" apply false
    id("app.cash.licensee") version "1.5.0" apply false
}

repositories {
    mavenCentral()
}

group = "app.softwork"

subprojects {
    if (this.name == "testing") {
        return@subprojects
    }

    plugins.apply("app.cash.licensee")
    configure<LicenseeExtension> {
        allow("Apache-2.0")
        allow("MIT")
        allowUrl("https://jdbc.postgresql.org/about/license.html")
    }

    afterEvaluate {
        configure<KotlinProjectExtension> {
            explicitApi()
            sourceSets {
                all {
                    languageSettings.progressiveMode = true
                }
            }
        }
    }

    plugins.apply("org.gradle.maven-publish")
    plugins.apply("org.gradle.signing")
    val emptyJar by tasks.creating(Jar::class) { }

    group = "app.softwork"

    publishing {
        publications.all {
            this as MavenPublication
            artifact(emptyJar) {
                classifier = "javadoc"
            }
            pom {
                name.set("app.softwork Postgres Native Driver and SqlDelight Dialect")
                description.set("A Postgres native driver including support for SqlDelight")
                url.set("https://github.com/hfhbd/kotlinx-serialization-csv")
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

    (System.getProperty("signing.privateKey") ?: System.getenv("SIGNING_PRIVATE_KEY"))?.let {
        String(Base64.getDecoder().decode(it)).trim()
    }?.let { key ->
        println("found key, config signing")
        signing {
            val signingPassword = System.getProperty("signing.password") ?: System.getenv("SIGNING_PASSWORD")
            useInMemoryPgpKeys(key, signingPassword)
            sign(publishing.publications)
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            username.set(System.getProperty("sonartype.apiKey") ?: System.getenv("SONARTYPE_APIKEY"))
            password.set(System.getProperty("sonartype.apiToken") ?: System.getenv("SONARTYPE_APITOKEN"))
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}
