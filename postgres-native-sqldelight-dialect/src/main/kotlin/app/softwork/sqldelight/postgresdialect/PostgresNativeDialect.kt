package app.softwork.sqldelight.postgresdialect

import app.cash.sqldelight.dialect.api.*
import app.cash.sqldelight.dialects.postgresql.*
import app.cash.sqldelight.dialects.postgresql.grammar.psi.*
import com.alecstrong.sql.psi.core.psi.*
import com.squareup.kotlinpoet.*

class PostgresNativeDialect : PostgreSqlDialect() {
    override val runtimeTypes = RuntimeTypes(
        driverType = ClassName("app.softwork.sqldelight.postgresdriver", "PostgresNativeDriver"),
        cursorType = ClassName("app.softwork.sqldelight.postgresdriver", "PostgresCursor"),
        preparedStatementType = ClassName("app.softwork.sqldelight.postgresdriver", "PostgresPreparedStatement")
    )

    override fun typeResolver(parentResolver: TypeResolver): TypeResolver = PostgresNativeTypeResolver(parentResolver)

    class PostgresNativeTypeResolver(parentResolver: TypeResolver) : PostgreSqlTypeResolver(parentResolver) {
        override fun definitionType(typeName: SqlTypeName): IntermediateType = with(typeName) {
            check(this is PostgreSqlTypeName)
            val type = IntermediateType(
                when {
                    smallIntDataType != null -> PostgreSqlType.SMALL_INT
                    intDataType != null -> PostgreSqlType.INTEGER
                    bigIntDataType != null -> PostgreSqlType.BIG_INT
                    approximateNumericDataType != null -> PrimitiveType.REAL
                    stringDataType != null -> PrimitiveType.TEXT
                    uuidDataType != null -> PostgreSqlType.UUID
                    smallSerialDataType != null -> PostgreSqlType.SMALL_INT
                    serialDataType != null -> PostgreSqlType.INTEGER
                    bigSerialDataType != null -> PostgreSqlType.BIG_INT
                    dateDataType != null -> {
                        when (dateDataType!!.firstChild.text) {
                            "DATE" -> PostgreSqlType.DATE
                            //"TIME" -> PostgreSqlType.TIME
                            "TIMESTAMP" -> if (dateDataType!!.node.getChildren(null)
                                    .any { it.text == "WITH" }
                            ) PostgreSqlType.TIMESTAMP_TIMEZONE else PostgreSqlType.TIMESTAMP
                            "TIMESTAMPTZ" -> PostgreSqlType.TIMESTAMP_TIMEZONE
                            "INTERVAL" -> PostgreSqlType.INTERVAL
                            else -> throw IllegalArgumentException("Unknown date type ${dateDataType!!.text}")
                        }
                    }
                    jsonDataType != null -> PrimitiveType.TEXT
                    booleanDataType != null -> PrimitiveType.BOOLEAN
                    blobDataType != null -> PrimitiveType.BLOB
                    else -> throw IllegalArgumentException("Unknown kotlin type for sql type ${this.text}")
                }
            )
            return type
        }
    }
}

internal enum class PostgreSqlType(override val javaType: TypeName): DialectType {
    SMALL_INT(SHORT) {
        override fun decode(value: CodeBlock) = CodeBlock.of("%L.toShort()", value)

        override fun encode(value: CodeBlock) = CodeBlock.of("%L.toLong()", value)
    },
    INTEGER(INT) {
        override fun decode(value: CodeBlock) = CodeBlock.of("%L.toInt()", value)

        override fun encode(value: CodeBlock) = CodeBlock.of("%L.toLong()", value)
    },
    BIG_INT(LONG),
    DATE(ClassName("kotlinx.datetime", "LocalDate")),
    //TIME(kotlinx.datetime.LocalTime::class.asTypeName()),
    TIMESTAMP(ClassName("kotlinx.datetime", "LocalDateTime")),
    TIMESTAMP_TIMEZONE(ClassName("kotlinx.datetime", "Instant")),
    INTERVAL(ClassName("kotlin.time", "Duration")),
    UUID(ClassName("kotlinx.uuid", "UUID"));

    override fun prepareStatementBinder(columnIndex: String, value: CodeBlock): CodeBlock {
        return CodeBlock.builder()
            .add(
                when (this) {
                    SMALL_INT, INTEGER, BIG_INT -> "bindLong"
                    DATE -> "bindDate"
                    //TIME -> "bindTime"
                    TIMESTAMP -> "bindLocalTimestamp"
                    TIMESTAMP_TIMEZONE -> "bindTimestamp"
                    INTERVAL -> "bindInterval"
                    UUID -> "bindUUID"
                }
            )
            .add("($columnIndex, %L)\n", value)
            .build()
    }

    override fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlock {
        return CodeBlock.of(
            when (this) {
                SMALL_INT, INTEGER, BIG_INT -> "$cursorName.getLong($columnIndex)"
                DATE -> "$cursorName.getDate($columnIndex)"
                //TIME -> "$cursorName.getTime($columnIndex)"
                TIMESTAMP -> "$cursorName.getLocalTimestamp($columnIndex)"
                TIMESTAMP_TIMEZONE -> "$cursorName.getTimestamp($columnIndex)"
                INTERVAL -> "$cursorName.getInterval($columnIndex)"
                UUID -> "$cursorName.getUUID($columnIndex)"
            }
        )
    }
}
