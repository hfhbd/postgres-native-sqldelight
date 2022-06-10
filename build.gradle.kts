import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.konan.target.*

plugins {
    kotlin("multiplatform") version "1.7.0"
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

    when (HostManager.host) {
        KonanTarget.MACOS_ARM64 -> {
            macosArm64 { config() }
        }
        KonanTarget.MACOS_X64 -> {
            macosX64 { config() }
        }
        KonanTarget.LINUX_X64 -> {
            linuxX64 { config() }
        }
        KonanTarget.MINGW_X64 -> {
            mingwX64 { config() }
        }
        else -> error("Not yet supported")
    }

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
