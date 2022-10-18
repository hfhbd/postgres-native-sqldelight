# Module postgres-native-sqldelight

A native Postgres driver for SqlDelight.

- [Source code](https://github.com/hfhbd/postgres-native-sqldelight)

## Install

You need `libpq` installed and available in your `$PATH`.

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

## Contributing

You need libpq installed: https://formulae.brew.sh/formula/libpq#default
You have to add the compiler flags to your path too.
The exact commands depend on your config, but you will get them during installing libpq with homebrew.

Sample commands:

```
If you need to have libpq first in your PATH, run:
  echo 'export PATH="/home/linuxbrew/.linuxbrew/opt/libpq/bin:$PATH"' >> /home/runner/.bash_profile
For compilers to find libpq you may need to set:
  export LDFLAGS="-L/home/linuxbrew/.linuxbrew/opt/libpq/lib"
  export CPPFLAGS="-I/home/linuxbrew/.linuxbrew/opt/libpq/include"
```

### Testing

If you install libpq with homebrew, it will install the platform-specific artifact.

| Host        | Supported test targets |
|-------------|------------------------|
| linux x64   | linux x64              |
| macOS x64   | macOS x64, linux x64   |
| macOS arm64 | macOS arm64            |
