pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}

plugins {
    id("MyRepos")
}

rootProject.name = "postgres-native-sqldelight"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":postgres-native-sqldelight-driver")
include(":postgres-native-sqldelight-dialect")

include(":testing")
include(":testing-sqldelight")
