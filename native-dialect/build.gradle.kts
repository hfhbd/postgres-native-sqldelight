plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    maven(url = "https://cache-redirector.jetbrains.com/intellij-dependencies")
}

dependencies {
    api("app.cash.sqldelight:postgresql-dialect:2.0.0-SNAPSHOT")

    compileOnly("app.cash.sqldelight:dialect-api:2.0.0-SNAPSHOT")

    compileOnly("com.jetbrains.intellij.platform:core-impl:211.7628.21")
    compileOnly("com.jetbrains.intellij.platform:lang-impl:211.7628.21")

    testImplementation("com.jetbrains.intellij.platform:core-impl:211.7628.21")
    testImplementation("com.jetbrains.intellij.platform:lang-impl:211.7628.21")
    testImplementation(kotlin("test-junit"))
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
