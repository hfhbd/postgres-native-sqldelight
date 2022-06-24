package app.softwork.sqldelight.postgresdriver

import kotlinx.datetime.*
import kotlinx.uuid.*
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class PostgresNativeDriverTest {
    @Test
    fun allTypes() {
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

        val foo = Foo(
            a = 42,
            b = "answer",
            date = LocalDate(2020, Month.DECEMBER, 12),
            time = LocalTime(12, 42, 0, 0),
            timestamp = LocalDateTime(2014, Month.AUGUST, 1, 12, 1, 2, 0),
            instant = Instant.fromEpochMilliseconds(10L),
            interval = 42.seconds,
            uuid = UUID.NIL
        )
        queries.create(foo)
        assertEquals(foo, queries.get().executeAsOne())
    }

    @Test
    fun copyTest() {
        val driver = PostgresNativeDriver(
            host = "localhost",
            port = 5432,
            user = "postgres",
            database = "postgres",
            password = "password"
        )
        val queries = NativePostgres(driver).fooQueries
        NativePostgres.Schema.migrate(driver, 0, NativePostgres.Schema.version)
        queries.copy()
        val result = driver.copy("42,answer,2020-12-12,12:42:00.0000,2014-08-01T12:01:02.0000,1970-01-01T00:00:00.010Z,PT42S,00000000-0000-0000-0000-000000000000")
        assertEquals(1, result)
        val foo = Foo(
            a = 42,
            b = "answer",
            date = LocalDate(2020, Month.DECEMBER, 12),
            time = LocalTime(12, 42, 0, 0),
            timestamp = LocalDateTime(2014, Month.AUGUST, 1, 12, 1, 2, 0),
            instant = Instant.fromEpochMilliseconds(10L),
            interval = 42.seconds,
            uuid = UUID.NIL,
        )
        assertEquals(foo, queries.get().executeAsOne())
    }
}
