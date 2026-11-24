package zel.game.webcommands

import zel.db.Sql
import javax.sql.DataSource

class JdbcWebCommandStore(
    dataSource: DataSource,
) : WebCommandStore {
    private val sql = Sql(dataSource)

    override fun pending(limit: Int): List<WebCommand> =
        sql.select(PENDING) { int(maxOf(1, limit)) }.rows { row ->
            val createdBy = row.getInt("created_by")
            val byUser = row.wasNull() || createdBy <= 0
            WebCommand(
                row.getLong("id"),
                row.getInt("player_id"),
                row.getString("kind"),
                row.getString("payload"),
                if (byUser) null else createdBy,
            )
        }

    override fun claim(
        id: Long,
        now: Int,
    ): Boolean =
        sql.write(CLAIM) {
            int(now)
            long(id)
        } == 1

    override fun finish(
        id: Long,
        result: WebCommandResult,
        now: Int,
    ) {
        sql.write(FINISH) {
            string(result.status())
            stringOrNull(result.result)
            int(now)
            long(id)
        }
    }

    override fun failInterrupted(): Int = sql.write(FAIL_INTERRUPTED) { string(INTERRUPTED) }

    internal companion object {
        const val PENDING =
            "SELECT id, player_id, kind, payload, created_by FROM web_commands" +
                " WHERE status = 'pending' AND processed_at IS NULL ORDER BY id LIMIT ?"
        const val CLAIM =
            "UPDATE web_commands SET processed_at = ?" +
                " WHERE id = ? AND status = 'pending' AND processed_at IS NULL"
        const val FINISH =
            "UPDATE web_commands SET status = ?, result = ?, processed_at = ?" +
                " WHERE id = ? AND status = 'pending'"
        const val FAIL_INTERRUPTED =
            "UPDATE web_commands SET status = 'failed', result = ?" +
                " WHERE status = 'pending' AND processed_at IS NOT NULL"
        const val INTERRUPTED =
            "Interrupted: the game stopped while applying it. Check the account before you send it again."
    }
}
