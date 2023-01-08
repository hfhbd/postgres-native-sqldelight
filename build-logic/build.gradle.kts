plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
    implementation("com.alecstrong:grammar-kit-composer:0.1.10")
    implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
    implementation("org.jetbrains.kotlinx.binary-compatibility-validator:org.jetbrains.kotlinx.binary-compatibility-validator.gradle.plugin:0.12.1")
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
    implementation("app.cash.sqldelight:gradle-plugin:2.0.0-alpha04")
    implementation("app.cash.licensee:licensee-gradle-plugin:1.6.0")
}

gradlePlugin {
    plugins {
        create("MyRepos") {
            id = "MyRepos"
            implementationClass = "MyRepos"
        }
        create("repos") {
            id = "repos"
            implementationClass = "Repos"
        }
    }
}
