# Module postgres-native-sqldelight

A native Postgres driver for SqlDelight. 

- [Source code](https://github.com/hfhbd/postgres-native-sqldelight)

## Install

This package is uploaded to MavenCentral and supports macOS, linuxX64.
Windows is currently not supported.


````kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("app.softwork:postgres-native-sqldelight-driver:LATEST")
}

sqldelight {
    database("NativePostgres") {
        dialect("app.softwork:postgres-native-sqldelight-dialect:LATEST")
    }
    linkSqlite = false
}
````

## License

Apache 2
