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
        assertEquals(0, driver.execute(null, "DROP TABLE IF EXISTS baz;", parameters = 0).value)
        assertEquals(
            0,
            driver.execute(null, "CREATE TABLE baz(a INT PRIMARY KEY, foo TEXT, b BYTEA);", parameters = 0).value
        )
        repeat(5) {
            val result = driver.execute(null, "INSERT INTO baz VALUES ($it)", parameters = 0)
            assertEquals(1, result.value)
        }

        val result = driver.execute(null, "INSERT INTO baz VALUES ($1, $2, $3), ($4, $5, $6)", parameters = 6) {
            bindLong(0, 5)
            bindString(1, "bar 0")
            bindBytes(2, byteArrayOf(1.toByte(), 2.toByte()))

            bindLong(3, 6)
            bindString(4, "bar 1")
            bindBytes(5, byteArrayOf(16.toByte(), 12.toByte()))
        }.value
        assertEquals(2, result)
        val notPrepared = driver.executeQuery(null, "SELECT * FROM baz LIMIT 1;", parameters = 0, mapper = {
            assertTrue(it.next())
            Simple(
                index = it.getLong(0)!!.toInt(),
                name = it.getString(1),
                byteArray = it.getBytes(2)
            )
        })
        assertEquals(Simple(0, null, null), notPrepared.value)
        val preparedStatement = driver.executeQuery(
            42,
            sql = "SELECT * FROM baz;",
            parameters = 0, binders = null,
            mapper = {
                buildList {
                    while (it.next()) {
                        add(
                            Simple(
                                index = it.getLong(0)!!.toInt(),
                                name = it.getString(1),
                                byteArray = it.getBytes(2)
                            )
                        )
                    }
                }
            }
        ).value

        assertEquals(7, preparedStatement.size)
        assertEquals(
            List(5) {
                Simple(it, null, null)
            } + listOf(
                Simple(5, "bar 0", byteArrayOf(1.toByte(), 2.toByte())),
                Simple(6, "bar 1", byteArrayOf(16.toByte(), 12.toByte())),
            ),
            preparedStatement
        )

        expect(7) {
            val cursorList = driver.executeQueryWithNativeCursor(
                -99,
                "SELECT * FROM baz",
                fetchSize = 4,
                parameters = 0,
                binders = null,
                mapper = {
                    buildList {
                        while (it.next()) {
                            add(
                                Simple(
                                    index = it.getLong(0)!!.toInt(),
                                    name = it.getString(1),
                                    byteArray = it.getBytes(2)
                                )
                            )
                        }
                    }
                }).value
            cursorList.size
        }

        expect(7) {
            val cursorList = driver.executeQueryWithNativeCursor(
                -5,
                "SELECT * FROM baz",
                fetchSize = 1,
                parameters = 0,
                binders = null,
                mapper = {
                    buildList {
                        while (it.next()) {
                            add(
                                Simple(
                                    index = it.getLong(0)!!.toInt(),
                                    name = it.getString(1),
                                    byteArray = it.getBytes(2)
                                )
                            )
                        }
                    }
                }).value
            cursorList.size
        }

        expect(0) {
            val cursorList = driver.executeQueryWithNativeCursor(
                -100,
                "SELECT * FROM baz WHERE a = -1",
                fetchSize = 1,
                parameters = 0,
                binders = null,
                mapper = {
                    buildList {
                        while (it.next()) {
                            add(
                                Simple(
                                    index = it.getLong(0)!!.toInt(),
                                    name = it.getString(1),
                                    byteArray = it.getBytes(2)
                                )
                            )
                        }
                    }
                }).value
            cursorList.size
        }
    }

    private data class Simple(val index: Int, val name: String?, val byteArray: ByteArray?) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true

            other as Simple

            if (index != other.index) return false
            if (name != other.name) return false
            if (byteArray != null) {
                if (other.byteArray == null) return false
                if (!byteArray.contentEquals(other.byteArray)) return false
            } else if (other.byteArray != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = index.hashCode()
            result = 31 * result + (name?.hashCode() ?: 0)
            result = 31 * result + (byteArray?.contentHashCode() ?: 0)
            return result
        }
    }

    @Test
    fun wrongCredentials() {
        assertFailsWith<IllegalArgumentException> {
            PostgresNativeDriver(
                host = "wrongHost",
                user = "postgres",
                database = "postgres",
                password = "password"
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PostgresNativeDriver(
                host = "localhost",
                user = "postgres",
                database = "postgres",
                password = "wrongPassword"
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PostgresNativeDriver(
                host = "localhost",
                user = "wrongUser",
                database = "postgres",
                password = "password"
            )
        }
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
        assertEquals(0, driver.execute(null, "DROP TABLE IF EXISTS copying;", parameters = 0).value)
        assertEquals(0, driver.execute(null, "CREATE TABLE copying(a int primary key);", parameters = 0).value)
        driver.execute(-42, "COPY copying FROM STDIN (FORMAT CSV);", 0)
        val results = driver.copy("1\n2\n")
        assertEquals(2, results)
    }
}
