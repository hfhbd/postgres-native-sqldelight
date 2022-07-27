import groovy.util.*

plugins {
    kotlin("jvm")
    `maven-publish`
    id("com.github.johnrengelman.shadow")
}

java {
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    maven(url = "https://cache-redirector.jetbrains.com/intellij-dependencies")
}

dependencies {
    api("app.cash.sqldelight:postgresql-dialect:2.0.0-alpha03")

    compileOnly("app.cash.sqldelight:dialect-api:2.0.0-alpha03")

    val idea = "222.3345.118"
    compileOnly("com.jetbrains.intellij.platform:core-impl:$idea")
    compileOnly("com.jetbrains.intellij.platform:lang-impl:$idea")

    testImplementation("com.jetbrains.intellij.platform:core-impl:$idea")
    testImplementation("com.jetbrains.intellij.platform:lang-impl:$idea")
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
