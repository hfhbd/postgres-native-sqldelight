import org.jetbrains.kotlin.gradle.plugin.mpp.*

plugins {
    kotlin("multiplatform")
    app.cash.licensee
    repos
    publish
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
                api("io.ktor:ktor-network:2.2.2")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                api("app.cash.sqldelight:runtime:2.0.0-alpha04")
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                api("app.softwork:kotlinx-uuid-core:0.0.17")
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
