import groovy.util.*
import org.jetbrains.grammarkit.tasks.*

plugins {
    kotlin("jvm")
    `maven-publish`
    id("com.alecstrong.grammar.kit.composer")
    id("licensee")
    id("publishing")
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

val idea = "222.4345.24"

grammarKit {
    intellijRelease.set(idea)
}

// https://youtrack.jetbrains.com/issue/IDEA-301677
val grammar = configurations.create("grammar") {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    compileOnly("app.cash.sqldelight:postgresql-dialect:2.0.0-alpha04")

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
    explicitApi()
    sourceSets {
        all {
            languageSettings.progressiveMode = true
        }
    }
}

configurations.all {
    exclude(group = "com.jetbrains.rd")
    exclude(group = "com.github.jetbrains", module = "jetCheck")
    exclude(group = "org.roaringbitmap")
}

tasks {
    val generateapp_softwork_sqldelight_postgresdialect_PostgreSqlNativeParser by getting(GenerateParserTask::class) {
        classpath.from(grammar)
    }
    generateParser {
        classpath.from(grammar)
    }
}
