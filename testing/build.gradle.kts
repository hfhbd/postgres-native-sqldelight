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
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.0-alpha04")
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
    database("NativePostgres") {
        dialect(projects.postgresNativeSqldelightDialect)
        packageName = "app.softwork.sqldelight.postgresdriver"
        deriveSchemaFromMigrations = true
    }
    linkSqlite = false
}
