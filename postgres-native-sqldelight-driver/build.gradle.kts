import org.jetbrains.kotlin.gradle.plugin.mpp.*

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    fun KotlinNativeTarget.config() {
        compilations.getByName("main") {
            cinterops {
                val libpq by creating {
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
                api("app.cash.sqldelight:runtime:2.0.0-alpha03")
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.3.3")
                api("app.softwork:kotlinx-uuid-core:0.0.15")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.3")
            }
        }
    }
}
