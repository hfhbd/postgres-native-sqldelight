import org.jetbrains.kotlin.konan.target.*

plugins {
    kotlin("multiplatform")
    id("app.cash.sqldelight") version "2.0.0-SNAPSHOT"
}

repositories {
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    maven(url = "https://cache-redirector.jetbrains.com/intellij-dependencies")
}

kotlin {
    when (HostManager.host) {
        KonanTarget.MACOS_ARM64 -> {
            macosArm64()
        }
        KonanTarget.MACOS_X64 -> {
            macosX64()
        }
        KonanTarget.LINUX_X64 -> {
            linuxX64()
        }
        KonanTarget.MINGW_X64 -> {
            mingwX64()
        }
        else -> error("Not yet supported")
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":"))
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

sqldelight {
    database("NativePostgres") {
        dialect(project(":native-dialect"))
        packageName = "app.softwork.sqldelight.postgresdriver"
        deriveSchemaFromMigrations = true
    }
}
