plugins {
    kotlin("multiplatform")
    id("app.cash.licensee")
    id("publish")
}

kotlin {
    targetHierarchy.default()
    
    jvm()
    macosArm64()
    macosX64()
    linuxX64()

    sourceSets {
        commonMain {
            dependencies {
                api(libs.sqldelight.runtime)
                api(libs.sqldelight.async)
                api(libs.datetime)
                api(libs.uuid)
            }
        }
        named("jvmMain") {
            dependencies {
                api(libs.sqldelight.jdbc.driver)
                api(libs.postgresql)
            }
        }

        named("nativeMain") {
            dependencies {
                api(projects.driver)
            }
        }
    }

    kotlin {
        jvmToolchain(8)
        
        explicitApi()
        sourceSets {
            configureEach {
                languageSettings.progressiveMode = true
            }
        }
    }
}

licensee {
    allow("Apache-2.0")
    allowUrl("https://jdbc.postgresql.org/about/license.html")
    allow("CC0-1.0")
}
