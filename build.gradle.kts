import org.jetbrains.kotlin.gradle.plugin.mpp.*

plugins {
    kotlin("multiplatform") version "1.7.0-Beta"
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

    macosX64 {
        config()
    }
    macosArm64 {
        config()
    }
    linuxX64 {
        config()
    }
    /*mingwX64 {
        config()
    }*/

    sourceSets {
        commonMain {
            dependencies {
                api("app.cash.sqldelight:runtime:2.0.0-alpha02")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
