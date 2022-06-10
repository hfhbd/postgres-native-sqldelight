pluginManagement {
    repositories {
        gradlePluginPortal()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
        maven(url = "https://www.jetbrains.com/intellij-repository/releases")
        maven(url = "https://cache-redirector.jetbrains.com/intellij-dependencies")
    }
}

rootProject.name = "SqlDelightNativePostgres"

include(":native-dialect")
include(":testing")
