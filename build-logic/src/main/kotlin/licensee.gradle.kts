plugins {
    id("app.cash.licensee")
}

repositories {
    mavenCentral()
}

licensee {
    allow("Apache-2.0")
    allow("MIT")
    allowUrl("https://jdbc.postgresql.org/about/license.html")
}
