package app.softwork.sqldelight.postgresdriver

import kotlin.test.*

class PostgresNativeDriverTest {
    @Test
    fun simpleTest() {
        val driver = PostgresNativeDriver(
            host = "localhost",
            port = 5432,
            user = "postgres",
            database = "postgres",
            password = "password"
        )
        val queries = NativePostgres(driver).fooQueries
        NativePostgres.Schema.migrate(driver, 0, NativePostgres.Schema.version)
        assertEquals(emptyList(), queries.get().executeAsList())

        queries.create(Foo(42, "answer"))
        assertEquals(Foo(42, "answer"), queries.get().executeAsOne())
    }
}
