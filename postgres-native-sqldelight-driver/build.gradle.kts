import org.jetbrains.kotlin.gradle.plugin.mpp.*

plugins {
    kotlin("multiplatform")
    app.cash.licensee
    repos
    publish
    org.jetbrains.dokka
}

kotlin {
    explicitApi()
    sourceSets {
        configureEach {
            languageSettings.progressiveMode = true
        }
    }

    fun KotlinNativeTarget.config() {
        compilations.named("main") {
            cinterops {
                register("libpq") {
                    defFile(project.file("src/nativeInterop/cinterop/libpq.def"))
                }
            }
        }
    }

    macosArm64 { config() }
    macosX64 { config() }
    linuxX64 { config() }
    // mingwX64 { config() }

    sourceSets {
        commonMain {
            dependencies {
                implementation("io.ktor:ktor-network:2.3.0")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0")
                api("app.cash.sqldelight:runtime:2.0.0-alpha05")
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                api("app.softwork:kotlinx-uuid-core:0.0.18")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
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
