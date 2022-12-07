package app.softwork.sqldelight.postgresdialect

import app.cash.sqldelight.dialect.api.*
import app.cash.sqldelight.dialects.postgresql.*
import app.cash.sqldelight.dialects.postgresql.grammar.psi.*
import com.alecstrong.sql.psi.core.psi.*
import com.squareup.kotlinpoet.*

public class PostgresJvmNixDialect : SqlDelightDialect by PostgresNativeDialect() {
    override val runtimeTypes: RuntimeTypes = RuntimeTypes(
        cursorType = ClassName("app.softwork.sqldelight.postgresdriver", "PostgresJvmNixCursor"),
        preparedStatementType = ClassName("app.softwork.sqldelight.postgresdriver", "PostgresJvmNixPreparedStatement")
    )
    override val asyncRuntimeTypes: RuntimeTypes get() = error("Native async driver support is not supported")
    override fun typeResolver(parentResolver: TypeResolver): TypeResolver = PostgresNativeTypeResolver(parentResolver)
}

private class PostgresNativeTypeResolver(parentResolver: TypeResolver) :
    TypeResolver by PostgreSqlTypeResolver(parentResolver) {
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
                        "TIME" -> PostgreSqlType.TIME
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

private enum class PostgreSqlType(override val javaType: TypeName) : DialectType {
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
    TIME(ClassName("kotlinx.datetime", "LocalTime")),
    TIMESTAMP(ClassName("kotlinx.datetime", "LocalDateTime")),
    TIMESTAMP_TIMEZONE(ClassName("kotlinx.datetime", "Instant")),
    INTERVAL(ClassName("kotlinx.datetime", "DateTimePeriod")),
    UUID(ClassName("kotlinx.uuid", "UUID"));

    override fun prepareStatementBinder(columnIndex: String, value: CodeBlock): CodeBlock = with(CodeBlock.builder()) {
        when (this@PostgreSqlType) {
            SMALL_INT, INTEGER, BIG_INT -> add("bindLong($columnIndex, %L)\n", value)
            DATE -> add(
                "%M($columnIndex, %L)\n",
                MemberName("app.softwork.sqldelight.postgresdriver", "bindDate", isExtension = true),
                value
            )
            TIME -> add(
                "%M($columnIndex, %L)\n",
                MemberName("app.softwork.sqldelight.postgresdriver", "bindTime", isExtension = true),
                value
            )
            TIMESTAMP -> add(
                "%M($columnIndex, %L)\n",
                MemberName("app.softwork.sqldelight.postgresdriver", "bindLocalTimestamp", isExtension = true),
                value
            )
            TIMESTAMP_TIMEZONE -> add(
                "%M($columnIndex, %L)\n",
                MemberName("app.softwork.sqldelight.postgresdriver", "bindTimestamp", isExtension = true),
                value
            )
            INTERVAL -> add(
                "%M($columnIndex, %L)\n",
                MemberName("app.softwork.sqldelight.postgresdriver", "bindInterval", isExtension = true),
                value
            )
            UUID -> add(
                "%M($columnIndex, %L)\n",
                MemberName("app.softwork.sqldelight.postgresdriver", "bindUUID", isExtension = true),
                value
            )
        }
    }.build()

    override fun cursorGetter(columnIndex: Int, cursorName: String) = with(CodeBlock.builder()) {
        when (this@PostgreSqlType) {
            SMALL_INT, INTEGER, BIG_INT -> add("$cursorName.getLong($columnIndex)")
            DATE -> add(
                "$cursorName.%M($columnIndex)", 
                MemberName("app.softwork.sqldelight.postgresdriver", "getDate", isExtension = true)
            )
            TIME -> add(
                "$cursorName.%M($columnIndex)",
                MemberName("app.softwork.sqldelight.postgresdriver", "getTime", isExtension = true)
            )
            TIMESTAMP -> add(
                "$cursorName.%M($columnIndex)",
                MemberName("app.softwork.sqldelight.postgresdriver", "getLocalTimestamp", isExtension = true)
            )
            TIMESTAMP_TIMEZONE -> add(
                "$cursorName.%M($columnIndex)",
                MemberName("app.softwork.sqldelight.postgresdriver", "getTimestamp", isExtension = true)
            )
            INTERVAL -> add(
                "$cursorName.%M($columnIndex)",
                MemberName("app.softwork.sqldelight.postgresdriver", "getInterval", isExtension = true)
            )
            UUID -> add(
                "$cursorName.%M($columnIndex)",
                MemberName("app.softwork.sqldelight.postgresdriver", "getUUID", isExtension = true)
            )
        }
    }.build()
}
