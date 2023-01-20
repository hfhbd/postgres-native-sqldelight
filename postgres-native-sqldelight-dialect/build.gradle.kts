plugins {
    kotlin("jvm")
    com.alecstrong.grammar.kit.composer
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
    api("app.cash.sqldelight:postgresql-dialect:2.0.0-alpha05")

    api("app.cash.sqldelight:dialect-api:2.0.0-alpha05")

    compileOnly("com.jetbrains.intellij.platform:analysis-impl:$idea")

    testImplementation("com.jetbrains.intellij.platform:analysis-impl:$idea")
    testImplementation(kotlin("test"))
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
