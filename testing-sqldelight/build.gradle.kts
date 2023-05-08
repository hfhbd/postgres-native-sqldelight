import org.jetbrains.kotlin.konan.target.*

plugins {
    kotlin("multiplatform")
    id("app.cash.sqldelight")
    id("repos")
}

kotlin {

    when (HostManager.host) {
        KonanTarget.LINUX_X64 -> linuxX64()
        KonanTarget.MACOS_ARM64 -> macosArm64()
        KonanTarget.MACOS_X64 -> macosX64()
        else -> error("Not supported")
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.postgresNativeSqldelightDriver)
                implementation(libs.sqldelight.coroutines)
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

sqldelight {
    databases.register("NativePostgres") {
        dialect(projects.postgresNativeSqldelightDialect)
        packageName.set("app.softwork.sqldelight.postgresdriver")
        deriveSchemaFromMigrations.set(true)
    }
    linkSqlite.set(false)
}
