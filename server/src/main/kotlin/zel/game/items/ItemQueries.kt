package zel.game.items

import zel.db.Sql
import zel.db.orFallback
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

/** A failure is logged and reads as nothing found. */
internal class ItemQueries(
    private val dataSource: DataSource,
) {
    private val sql = Sql(dataSource)

    fun <T : Any> withConnection(work: (Connection) -> T?): T? =
        LOGGER.orFallback("item query", null) { dataSource.connection.use(work) }

    fun forEachRow(
        query: String,
        vararg values: Any,
        onRow: (ResultSet) -> Unit,
    ) = LOGGER.orFallback("item query", Unit) { sql.select(query, *values).each(onRow) }

    fun <T : Any> firstRow(
        query: String,
        vararg values: Any,
        read: (ResultSet) -> T,
    ): T? = LOGGER.orFallback("item query", null) { sql.select(query, *values).first(read) }

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(ItemQueries::class.java)
    }
}
