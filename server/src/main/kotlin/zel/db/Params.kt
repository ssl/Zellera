package zel.db

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement

class Params(
    private val statement: PreparedStatement,
) {
    private var column = 0

    fun int(value: Int) = statement.setInt(++column, value)

    fun intOrNull(
        value: Int,
        none: Int = 0,
    ) = statement.setNoneAsNull(++column, value, none)

    fun intOrNullWhenNotPositive(value: Int) = statement.setNotPositiveAsNull(++column, value)

    fun string(value: String?) = statement.setString(++column, value)

    fun stringOrNull(value: String?) = statement.setEmptyAsNull(++column, value)

    fun boolean(value: Boolean) = statement.setBoolean(++column, value)

    fun double(value: Double) = statement.setDouble(++column, value)

    fun long(value: Long) = statement.setLong(++column, value)

    fun value(value: Any?) = statement.setObject(++column, value)
}

// The Connection helpers join the caller's transaction; Sql opens a connection of its own per statement.

fun <T> Connection.rows(
    sql: String,
    bind: Params.() -> Unit = {},
    read: (ResultSet) -> T,
): List<T> =
    prepareStatement(sql).use { statement ->
        Params(statement).bind()
        statement.executeQuery().use { it.mapRows(read) }
    }

fun <T : Any> Connection.first(
    sql: String,
    bind: Params.() -> Unit = {},
    read: (ResultSet) -> T?,
): T? =
    prepareStatement(sql).use { statement ->
        Params(statement).bind()
        statement.executeQuery().use { if (it.next()) read(it) else null }
    }

/** Statements without parameters, in order on one statement handle. */
fun Connection.executeAll(vararg statements: String) {
    createStatement().use { handle -> statements.forEach { handle.execute(it) } }
}

fun Connection.write(
    sql: String,
    bind: Params.() -> Unit = {},
): Int =
    prepareStatement(sql).use { statement ->
        Params(statement).bind()
        statement.executeUpdate()
    }

fun <T> Connection.writeEach(
    sql: String,
    items: Iterable<T>,
    bind: Params.(T) -> Unit,
): IntArray =
    prepareStatement(sql).use { statement ->
        items.forEach { item ->
            Params(statement).bind(item)
            statement.addBatch()
        }
        statement.executeBatch()
    }

fun Connection.insertForId(
    sql: String,
    bind: Params.() -> Unit = {},
): Int? =
    prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { statement ->
        Params(statement).bind()
        statement.executeUpdate()
        statement.generatedKeys.use { if (it.next()) it.getInt(1) else null }
    }

fun <T> ResultSet.mapRows(map: (ResultSet) -> T): List<T> = buildList { while (next()) add(map(this@mapRows)) }
