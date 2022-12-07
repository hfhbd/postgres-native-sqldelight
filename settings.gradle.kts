pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}

plugins {
    id("MyRepos")
    id("com.gradle.enterprise") version "3.13.1"
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

include(":driver")
include(":sqldelight-dialect")

include(":sqldelight-dialect-jvmNix")
include(":jvmNix-driver")

include(":testing")
include(":testing-sqldelight")
include(":testing-sqldelight-jvmNix")
