import org.jetbrains.kotlin.konan.target.*

plugins {
    kotlin("multiplatform")
    app.cash.sqldelight
    repos
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
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.0-alpha05")
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

sqldelight {
    databases.register("NativePostgres") {
        dialect(projects.postgresNativeSqldelightDialect)
        packageName.set("app.softwork.sqldelight.postgresdriver")
        deriveSchemaFromMigrations.set(true)
    }
    linkSqlite.set(false)
}
