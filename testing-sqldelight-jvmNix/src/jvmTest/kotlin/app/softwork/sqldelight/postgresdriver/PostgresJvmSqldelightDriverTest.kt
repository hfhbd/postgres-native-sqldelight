package app.softwork.sqldelight.postgresdriver

import app.cash.sqldelight.driver.jdbc.*
import kotlinx.coroutines.*
import kotlinx.datetime.*
import kotlinx.uuid.*
import org.postgresql.core.*
import org.postgresql.ds.*
import kotlin.test.*

@ExperimentalCoroutinesApi
class PostgresJvmSqldelightDriverTest {

    private val driver =
        PGSimpleDataSource().apply {
            serverNames = arrayOf("localhost")
            portNumbers = intArrayOf(5432)
            user = "postgres"
            password = "password"
            databaseName = "postgres"
        }.asJdbcDriver()

    @Test
    fun allTypes() {
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
            interval = DateTimePeriod(42, 42, 42, 42, 42, 42, 424242),
            uuid = UUID.NIL
        )
        queries.create(
            a = foo.a,
            b = foo.b,
            date = foo.date,
            time = foo.time,
            timestamp = foo.timestamp,
            instant = foo.instant,
            interval = foo.interval,
            uuid = foo.uuid
        )
        assertEquals(foo, queries.get().executeAsOne())
    }

    @Test
    fun copyTest() {
        val queries = NativePostgres(driver).fooQueries
        NativePostgres.Schema.migrate(driver, 0, NativePostgres.Schema.version)
        println("START COPY")
        queries.startCopy()
        println("BEGIN COPY")
        val result = (driver.getConnection() as BaseConnection).copyAPI.copyIn(
            "",
            "42,answer,2020-12-12,12:42:00.0000,2014-08-01T12:01:02.0000,1970-01-01T00:00:00.010Z,PT42S,00000000-0000-0000-0000-000000000000".reader()
        )
        println("END COPY")
        assertEquals(1, result)
        val foo = Foo(
            a = 42,
            b = "answer",
            date = LocalDate(2020, Month.DECEMBER, 12),
            time = LocalTime(12, 42, 0, 0),
            timestamp = LocalDateTime(2014, Month.AUGUST, 1, 12, 1, 2, 0),
            instant = Instant.fromEpochMilliseconds(10L),
            interval = DateTimePeriod(42, 42, 42, 42, 42, 42, 424242),
            uuid = UUID.NIL,
        )
        assertEquals(foo, queries.get().executeAsOne())
    }

    @Test
    fun userTest() {
        val queries = NativePostgres(driver).usersQueries
        NativePostgres.Schema.migrate(driver, 0, NativePostgres.Schema.version)
        val id = queries.insertAndGet("test@test", "test", "bio", "", null).executeAsOne()
        assertEquals(1, id)
        val id2 = queries.insertAndGet("test2@test", "test2", "bio2", "", null).executeAsOne()
        assertEquals(2, id2)
        val testUser = queries.selectByUsername("test").executeAsOne()
        assertEquals(
            SelectByUsername(
                "test@test", "test", "bio", ""
            ), testUser
        )
    }
}
