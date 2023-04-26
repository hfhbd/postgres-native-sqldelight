plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.21")
    implementation("com.alecstrong:grammar-kit-composer:0.1.12")
    implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
    implementation("org.jetbrains.kotlinx:binary-compatibility-validator:0.13.1")
    implementation("app.cash.sqldelight:gradle-plugin:2.0.0-alpha05")
    implementation("app.cash.licensee:licensee-gradle-plugin:1.6.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.8.10")
}

kotlin.jvmToolchain(17)
