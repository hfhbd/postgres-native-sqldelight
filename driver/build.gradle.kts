import org.jetbrains.kotlin.gradle.plugin.mpp.*

plugins {
    kotlin("multiplatform")
    id("app.cash.licensee")
    id("repos")
    id("publish")
    id("org.jetbrains.dokka")
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
                implementation(libs.ktor.network)
                api(libs.coroutines.core)
                api(libs.sqldelight.runtime)
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
