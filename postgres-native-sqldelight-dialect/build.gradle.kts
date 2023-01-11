import groovy.util.*
import org.jetbrains.grammarkit.tasks.*

plugins {
    kotlin("jvm")
    com.alecstrong.grammar.kit.composer
    com.github.johnrengelman.shadow
    org.jetbrains.kotlinx.`binary-compatibility-validator`
    app.cash.licensee
    repos
    publish
    exclude
}

java {
    withJavadocJar()
    withSourcesJar()
}

val idea = "222.4459.24"

grammarKit {
    intellijRelease.set(idea)
}

dependencies {
    api("app.cash.sqldelight:postgresql-dialect:2.0.0-alpha04")

    compileOnly("app.cash.sqldelight:dialect-api:2.0.0-alpha04")

    compileOnly("com.jetbrains.intellij.platform:core-impl:$idea")
    compileOnly("com.jetbrains.intellij.platform:util-ui:$idea")
    compileOnly("com.jetbrains.intellij.platform:project-model-impl:$idea")
    compileOnly("com.jetbrains.intellij.platform:analysis-impl:$idea")

    testImplementation("com.jetbrains.intellij.platform:core-impl:$idea")
    testImplementation("com.jetbrains.intellij.platform:util-ui:$idea")
    testImplementation("com.jetbrains.intellij.platform:project-model-impl:$idea")
    testImplementation("com.jetbrains.intellij.platform:analysis-impl:$idea")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(11)

    target.compilations.all {
        kotlinOptions.allWarningsAsErrors = true
    }
    explicitApi()
    sourceSets {
        all {
            languageSettings.progressiveMode = true
        }
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    include("*.jar")
    include("app/cash/sqldelight/**")
    include("app/softwork/sqldelight/postgresdialect/**")
    include("META-INF/services/*")
}

tasks.jar.configure {
    // Prevents shadowJar (with classifier = '') and this task from writing to the same path.
    enabled = false
}

configurations {
    fun conf(it: Configuration) {
        it.outgoing.artifacts.removeIf { it.buildDependencies.getDependencies(null).contains(tasks.jar.get()) }
        it.outgoing.artifact(tasks.shadowJar)
    }
    apiElements.configure {
        conf(this)
    }
    runtimeElements.configure { conf(this) }
}

artifacts {
    runtimeOnly(tasks.shadowJar)
    archives(tasks.shadowJar)
}

// Disable Gradle module.json as it lists wrong dependencies
tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

// Remove dependencies from POM: uber jar has no dependencies
publishing {
    publications {
        withType<MavenPublication> {
            if (name == "pluginMaven") {
                pom.withXml {
                    val pomNode = asNode()

                    val dependencyNodes: NodeList = pomNode.get("dependencies") as NodeList
                    dependencyNodes.forEach {
                        (it as Node).parent().remove(it)
                    }
                }
            }
            artifact(tasks.emptyJar) {
                classifier = "sources"
            }
        }
        create("shadow", MavenPublication::class.java) {
            project.shadow.component(this)
        }
    }
}

licensee {
    allow("Apache-2.0")
    allow("MIT")
    allowUrl("https://jdbc.postgresql.org/about/license.html")
}
