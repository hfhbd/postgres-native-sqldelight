import org.jetbrains.kotlin.konan.target.*

plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64()
    linuxArm64()
    macosX64()
    macosArm64()

    sourceSets {
        commonTest {
            dependencies {
                implementation(projects.postgresNativeSqldelightDriver)
                implementation(kotlin("test"))
                implementation(libs.coroutines.test)
            }
        }
    }
}
