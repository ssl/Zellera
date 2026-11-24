package zel.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.orFallback
import javax.sql.DataSource

/** Without a database nothing is read. */
internal class NameValueTable(
    private val table: String,
    private val dataSource: () -> DataSource?,
) {
    fun each(take: (name: String, value: String) -> Unit) =
        onTable("read", Unit) { sql ->
            sql.select("SELECT `name`, `value` FROM $table").each { take(it.getString(1), it.getString(2)) }
        }

    private inline fun <T> onTable(
        what: String,
        fallback: T,
        work: (Sql) -> T,
    ): T {
        val pool = dataSource() ?: return fallback
        return log.orFallback("$table $what", fallback) { work(Sql(pool)) }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(NameValueTable::class.java)
    }
}
