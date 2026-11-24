package zel.core

import zel.db.Sql
import java.sql.SQLException
import javax.sql.DataSource

/** The moment, in seconds since 1970, a recurring chore was last done. */
interface ChoreMarks {
    @Throws(SQLException::class)
    fun read(chore: String): Long?

    @Throws(SQLException::class)
    fun write(
        chore: String,
        doneAt: Long,
    )
}

class JdbcChoreMarks(
    private val dataSource: DataSource?,
) : ChoreMarks {
    override fun read(chore: String): Long? =
        dataSource?.let { pool ->
            Sql(pool).select("SELECT done_at FROM daily_chores WHERE chore = ?", chore).first { row -> row.getLong(1) }
        }

    override fun write(
        chore: String,
        doneAt: Long,
    ) {
        val pool = dataSource ?: return
        Sql(pool).write("REPLACE INTO daily_chores (chore, done_at) VALUES (?, ?)", chore, doneAt)
    }
}
