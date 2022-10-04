plugins {
    kotlin("multiplatform")
    id("app.cash.sqldelight")
}

repositories {
    mavenCentral()
}

kotlin {

    macosArm64()
    macosX64()

    linuxX64()
    // mingwX64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.postgresNativeSqldelightDriver)
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
        dialect(projects.postgresNativeSqldelightDialect)
        packageName = "app.softwork.sqldelight.postgresdriver"
        deriveSchemaFromMigrations = true
    }
    linkSqlite = false
}
