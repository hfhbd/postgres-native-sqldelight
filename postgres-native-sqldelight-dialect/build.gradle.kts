import groovy.util.*
import org.jetbrains.grammarkit.tasks.*

plugins {
    kotlin("jvm")
    `maven-publish`
    id("com.github.johnrengelman.shadow")
    id("com.alecstrong.grammar.kit.composer")
}

java {
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    maven(url = "https://cache-redirector.jetbrains.com/intellij-dependencies")
    maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies/")
    maven(url = "https://maven.pkg.jetbrains.space/public/p/ktor/eap")
}

val idea = "223.7571.182"

grammarKit {
    intellijRelease.set(idea)
}

// https://youtrack.jetbrains.com/issue/IDEA-301677
val grammar = configurations.create("grammar") {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    api("app.cash.sqldelight:postgresql-dialect:2.0.0-alpha04")

    compileOnly("app.cash.sqldelight:dialect-api:2.0.0-alpha04")

    grammar("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    compileOnly("com.jetbrains.intellij.platform:core-impl:$idea")
    compileOnly("com.jetbrains.intellij.platform:util-ui:$idea")
    compileOnly("com.jetbrains.intellij.platform:project-model-impl:$idea")
    compileOnly("com.jetbrains.intellij.platform:analysis-impl:$idea")

    testImplementation("com.jetbrains.intellij.platform:core-impl:$idea")
    testImplementation("com.jetbrains.intellij.platform:util-ui:$idea")
    testImplementation("com.jetbrains.intellij.platform:project-model-impl:$idea")
    testImplementation("com.jetbrains.intellij.platform:analysis-impl:$idea")
    testImplementation(kotlin("test-junit"))
}

kotlin {
    target.compilations.all {
        kotlinOptions.allWarningsAsErrors = true
    }
}

configurations.all {
    exclude(group = "com.jetbrains.rd")
    exclude(group = "com.github.jetbrains", module = "jetCheck")
    exclude(group = "org.roaringbitmap")
    exclude(group = "com.jetbrains.intellij.remoteDev")
    exclude(group = "com.jetbrains.intellij.spellchecker")
}

tasks {
    val generateapp_softwork_sqldelight_postgresdialect_PostgreSqlNativeParser by getting(GenerateParserTask::class) {
        classpath.from(grammar)
    }
    generateParser {
        classpath.from(grammar)
    }
}

tasks.shadowJar {
    classifier = ""
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
