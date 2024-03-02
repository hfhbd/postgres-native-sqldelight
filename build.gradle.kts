plugins {
    id("org.jetbrains.dokka")
}

tasks.dokkaHtmlMultiModule {
    includes.from("README.md")
}
