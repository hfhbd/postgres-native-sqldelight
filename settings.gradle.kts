rootProject.name = "postgres-native-sqldelight"

includeBuild("build-logic")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":postgres-native-sqldelight-driver")
include(":postgres-native-sqldelight-dialect")
include(":testing")
