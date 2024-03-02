import org.jetbrains.kotlin.konan.target.*

plugins {
    kotlin("multiplatform")
    id("app.cash.sqldelight")
}

kotlin {
    linuxX64()
    linuxArm64()
    macosX64()
    macosArm64()

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
