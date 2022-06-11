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
