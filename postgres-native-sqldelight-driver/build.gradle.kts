plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("app.cash.licensee")
    id("publish")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

kotlin {
    explicitApi()
    sourceSets {
        configureEach {
            languageSettings.progressiveMode = true
        }
    }

    targetHierarchy.default()
    jvm()
    macosArm64()
    macosX64()
    linuxX64()
    // linuxArm64 { config() }
    // mingwX64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.ktor.network)
                api(libs.coroutines.core)
                api(libs.sqldelight.runtime)
                api(libs.serialization.core)
                api(libs.datetime)
                api(libs.uuid)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutines.test)
            }
        }
    }
}

licensee {
    allow("Apache-2.0")
}

tasks.dokkaHtmlPartial {
    dokkaSourceSets.configureEach {
        externalDocumentationLink("https://cashapp.github.io/sqldelight/2.0.0-alpha05/2.x/")
        externalDocumentationLink(
            url = "https://kotlinlang.org/api/kotlinx-datetime/",
            packageListUrl = "https://kotlinlang.org/api/kotlinx-datetime/kotlinx-datetime/package-list",
        )
        externalDocumentationLink("https://uuid.softwork.app")
        externalDocumentationLink("https://kotlinlang.org/api/kotlinx.coroutines/")
    }
}
