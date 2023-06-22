plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.plugins.kotlin.jvm.toDep())
    implementation(libs.plugins.kotlin.serialization.toDep())
    implementation(libs.plugins.grammarKit.toDep())
    implementation(libs.plugins.publish.toDep())
    implementation(libs.plugins.binary.toDep())
    implementation(libs.plugins.sqldelight.toDep())
    implementation(libs.plugins.licensee.toDep())
    implementation(libs.plugins.dokka.toDep())
}

fun Provider<PluginDependency>.toDep() = map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}

kotlin.jvmToolchain(17)
