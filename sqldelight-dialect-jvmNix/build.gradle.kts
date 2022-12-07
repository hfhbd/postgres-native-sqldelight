plugins {
    kotlin("jvm")
    id("app.cash.licensee")
    id("repos")
    id("publish")
    id("exclude")
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    api(projects.sqldelightDialect)
    
    compileOnly(libs.intellij.analysis)

    testImplementation(libs.intellij.analysis)
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

publishing {
    publications.register<MavenPublication>("maven") {
        from(components["java"])
    }
}
