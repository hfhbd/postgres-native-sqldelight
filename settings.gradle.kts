pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}

plugins {
    id("MyRepos")
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
    id("com.gradle.enterprise") version "3.15.1"
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        if (System.getenv("CI") != null) {
            publishAlways()
            tag("CI")
        }
    }
}

rootProject.name = "postgres-native-sqldelight"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

include(":postgres-native-sqldelight-driver")
include(":postgres-native-sqldelight-dialect")

include(":testing")
include(":testing-sqldelight")
