plugins {
    kotlin("jvm")
    id("com.alecstrong.grammar.kit.composer")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("app.cash.licensee")
    id("repos")
    id("publish")
    id("exclude")
}

java {
    withJavadocJar()
    withSourcesJar()
}

grammarKit {
    intellijRelease.set(libs.versions.idea)
}

dependencies {
    api(libs.sqldelight.postgresql.dialect)

    api(libs.sqldelight.dialect.api)

    compileOnly(libs.intellij.analysis)

    testImplementation(kotlin("test"))
    testImplementation(libs.intellij.analysis)
}

kotlin {
    jvmToolchain(11)

    target.compilations.configureEach {
        kotlinOptions.allWarningsAsErrors = true
    }
    explicitApi()
    sourceSets {
        configureEach {
            languageSettings.progressiveMode = true
        }
    }
}

licensee {
    allow("Apache-2.0")
    allow("MIT")
    allowUrl("https://jdbc.postgresql.org/about/license.html")
}

publishing {
    publications.register<MavenPublication>("maven") {
        from(components["java"])
    }
}
