package app.softwork.sqldelight.postgresdriver

import app.cash.sqldelight.coroutines.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.*
import kotlinx.uuid.*
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
class PostgresNativeDriverTest {
    private val driver = PostgresNativeDriver(
        host = "localhost",
        port = 5432,
        user = "postgres",
        database = "postgres",
        password = "password"
    )

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
            interval = DateTimePeriod(42, 42, 42, 42, 42, 42, 424242000),
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
        queries.startCopy()
        val result =
            driver.copy("42,answer,2020-12-12,12:42:00.0000,2014-08-01T12:01:02.0000,1970-01-01T00:00:00.010Z,PT42S,00000000-0000-0000-0000-000000000000")
        assertEquals(1, result)
        val foo = Foo(
            a = 42,
            b = "answer",
            date = LocalDate(2020, Month.DECEMBER, 12),
            time = LocalTime(12, 42, 0, 0),
            timestamp = LocalDateTime(2014, Month.AUGUST, 1, 12, 1, 2, 0),
            instant = Instant.fromEpochMilliseconds(10L),
            interval = DateTimePeriod(42, 42, 42, 42, 42, 42, 424242000),
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
                "test@test",
                "test",
                "bio",
                ""
            ),
            testUser
        )
    }

    @Test
    fun remoteListenerTest() = runTest(dispatchTimeoutMs = 10.seconds.inWholeMilliseconds) {
        val client = PostgresNativeDriver(
            host = "localhost",
            port = 5432,
            user = "postgres",
            database = "postgres",
            password = "password",
            listenerSupport = ListenerSupport.Remote(backgroundScope) {
                it + it
            }
        )

        val server = PostgresNativeDriver(
            host = "localhost",
            port = 5432,
            user = "postgres",
            database = "postgres",
            password = "password",
            listenerSupport = ListenerSupport.Remote(backgroundScope) {
                it + it
            }
        )

        val db = NativePostgres(client)
        NativePostgres.Schema.migrate(driver, 0, NativePostgres.Schema.version)

        db.fooQueries.create(
            a = 42,
            b = "answer",
            date = LocalDate(2020, Month.DECEMBER, 12),
            time = LocalTime(12, 42, 0, 0),
            timestamp = LocalDateTime(2014, Month.AUGUST, 1, 12, 1, 2, 0),
            instant = Instant.fromEpochMilliseconds(10L),
            interval = DateTimePeriod(42, 42, 42, 42, 42, 42, 424242),
            uuid = UUID.NIL
        )
        val userQueries = db.usersQueries
        val id = userQueries.insertAndGet("foo", "foo", "foo", "", 42).executeAsOne()

        val users = async {
            db.usersQueries.selectByFoo(42)
                .asFlow().mapToOne(coroutineContext)
                .take(2).toList()
        }
        withContext(Dispatchers.Default) {
            val waitForRemoteNotifications = 2.seconds
            delay(waitForRemoteNotifications)
        }
        runCurrent()

        NativePostgres(server).usersQueries.updateWhereFoo("foo2", 42)
        withContext(Dispatchers.Default) {
            val waitForRemoteNotifications = 2.seconds
            delay(waitForRemoteNotifications)
        }
        runCurrent()

        assertEquals(
            listOf(
                Users(
                    id,
                    "foo",
                    "foo",
                    "foo",
                    "",
                    42
                ), Users(
                    id,
                    "foo2",
                    "foo",
                    "foo",
                    "",
                    42
                )
            ), users.await()
        )

        client.close()
        server.close()
    }

    @Test
    fun localListenerTest() = runTest(dispatchTimeoutMs = 10.seconds.inWholeMilliseconds) {
        val client = PostgresNativeDriver(
            host = "localhost",
            port = 5432,
            user = "postgres",
            database = "postgres",
            password = "password",
            listenerSupport = ListenerSupport.Local(backgroundScope)
        )

        val db = NativePostgres(client)
        NativePostgres.Schema.migrate(driver, 0, NativePostgres.Schema.version)

        db.fooQueries.create(
            a = 42,
            b = "answer",
            date = LocalDate(2020, Month.DECEMBER, 12),
            time = LocalTime(12, 42, 0, 0),
            timestamp = LocalDateTime(2014, Month.AUGUST, 1, 12, 1, 2, 0),
            instant = Instant.fromEpochMilliseconds(10L),
            interval = DateTimePeriod(42, 42, 42, 42, 42, 42, 424242),
            uuid = UUID.NIL
        )
        val userQueries = db.usersQueries
        val id = userQueries.insertAndGet("foo", "foo", "foo", "", 42).executeAsOne()

        val users = async {
            db.usersQueries.selectByFoo(42)
                .asFlow().mapToOne(coroutineContext)
                .take(2).toList()
        }
        runCurrent()

        userQueries.updateWhereFoo("foo2", 42)
        runCurrent()

        assertEquals(
            listOf(
                Users(
                    id,
                    "foo",
                    "foo",
                    "foo",
                    "",
                    42
                ), Users(
                    id,
                    "foo2",
                    "foo",
                    "foo",
                    "",
                    42
                )
            ), users.await()
        )

        client.close()
    }
}
