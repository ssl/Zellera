package zel.db

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import javax.sql.DataSource

class Sql(
    private val pool: DataSource,
) {
    inner class Selection internal constructor(
        private val sql: String,
        private val bind: Params.() -> Unit,
    ) {
        fun <T> rows(read: (ResultSet) -> T): List<T> = reading { it.mapRows(read) }

        fun <T : Any> first(read: (ResultSet) -> T?): T? = reading { if (it.next()) read(it) else null }

        fun each(read: (ResultSet) -> Unit) = reading { while (it.next()) read(it) }

        fun <T> result(read: (ResultSet) -> T): T = reading(read)

        private inline fun <R> reading(crossinline use: (ResultSet) -> R): R =
            prepared(sql, bind) { prepared -> prepared.executeQuery().use { use(it) } }
    }

    fun select(
        sql: String,
        vararg values: Any?,
    ): Selection = Selection(sql) { values.forEach { value(it) } }

    fun select(
        sql: String,
        bind: Params.() -> Unit,
    ): Selection = Selection(sql, bind)

    fun write(
        sql: String,
        vararg values: Any?,
    ): Int = prepared(sql, { values.forEach { value(it) } }) { it.executeUpdate() }

    fun write(
        sql: String,
        bind: Params.() -> Unit,
    ): Int = prepared(sql, bind) { it.executeUpdate() }

    fun <T> writeEach(
        sql: String,
        items: Iterable<T>,
        bind: Params.(T) -> Unit,
    ): IntArray =
        prepared(sql, {}) { prepared ->
            items.forEach { item ->
                Params(prepared).bind(item)
                prepared.addBatch()
            }
            prepared.executeBatch()
        }

    private inline fun <R> prepared(
        sql: String,
        bind: Params.() -> Unit,
        run: (PreparedStatement) -> R,
    ): R =
        try {
            pool.connection.use { connection ->
                connection.prepareStatement(sql).use { prepared ->
                    Params(prepared).bind()
                    run(prepared)
                }
            }
        } catch (failure: SQLException) {
            throw SqlFailure(sql, failure)
        }
}
