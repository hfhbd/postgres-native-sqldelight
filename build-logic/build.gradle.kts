plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.grammarKit.gradlePlugin)
    implementation(libs.publish.gradlePlugin)
    implementation(libs.binary.gradlePlugin)
    implementation(libs.sqldelight.gradlePlugin)
    implementation(libs.licensee.gradlePlugin)
    implementation(libs.dokka.gradlePlugin)
}

kotlin.jvmToolchain(17)
