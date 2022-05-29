package app.softwork.sqldelight.postgresdialect

import app.cash.sqldelight.dialect.api.*
import app.cash.sqldelight.dialects.postgresql.*
import app.cash.sqldelight.dialects.postgresql.grammar.psi.*
import com.alecstrong.sql.psi.core.psi.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

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
                    numericDataType != null -> PostgreSqlType.NUMERIC
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
            if (node.getChildren(null).map { it.text }.takeLast(2) == listOf("[", "]")) {
                return IntermediateType(object : DialectType {
                    override val javaType = Array::class.asTypeName().parameterizedBy(type.javaType)

                    override fun prepareStatementBinder(columnIndex: String, value: CodeBlock) =
                        CodeBlock.of("bindObject($columnIndex, %L)\n", value)

                    override fun cursorGetter(columnIndex: Int, cursorName: String) =
                        CodeBlock.of("$cursorName.getArray($columnIndex)")
                })
            }
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
    DATE(ClassName("java.time", "LocalDate")),
    TIME(ClassName("java.time", "LocalTime")),
    TIMESTAMP(ClassName("java.time", "LocalDateTime")),
    TIMESTAMP_TIMEZONE(ClassName("java.time", "OffsetDateTime")),
    INTERVAL(ClassName("org.postgresql.util", "PGInterval")),
    UUID(ClassName("java.util", "UUID")),
    NUMERIC(ClassName("java.math", "BigDecimal")),
    ;

    override fun prepareStatementBinder(columnIndex: String, value: CodeBlock): CodeBlock {
        return CodeBlock.builder()
            .add(
                when (this) {
                    SMALL_INT, INTEGER, BIG_INT -> "bindLong"
                    DATE, TIME, TIMESTAMP, TIMESTAMP_TIMEZONE, INTERVAL, UUID -> "bindObject"
                    NUMERIC -> "bindBigDecimal"
                }
            )
            .add("($columnIndex, %L)\n", value)
            .build()
    }

    override fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlock {
        return CodeBlock.of(
            when (this) {
                SMALL_INT, INTEGER, BIG_INT -> "$cursorName.getLong($columnIndex)"
                DATE, TIME, TIMESTAMP, TIMESTAMP_TIMEZONE, INTERVAL, UUID -> "$cursorName.getObject($columnIndex)"
                NUMERIC -> "$cursorName.getBigDecimal($columnIndex)"
            }
        )
    }
}
