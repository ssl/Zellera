package zel.wired.score

import zel.db.Sql
import zel.db.insertForId
import zel.db.rows
import zel.db.write
import zel.db.writeEach
import zel.room.furni.buildpass.BuildPassRowIds
import zel.wired.store.inOneTransaction
import javax.sql.DataSource

class JdbcHighscoreStore(
    private val dataSource: DataSource,
) : HighscoreStore {
    private val sql = Sql(dataSource)

    override fun entries(
        boardId: Int,
        since: Int,
    ): List<ScoreEntry> =
        dataSource.connection.use { c ->
            val row = BuildPassRowIds.rowOf(c, boardId)
            c.rows(ENTRIES, {
                int(row)
                int(since)
            }) { rs ->
                ScoreEntry(
                    rs
                        .getString("player_ids")
                        .orEmpty()
                        .split(SEPARATOR)
                        .mapNotNull { it.trim().toIntOrNull() },
                    rs.getInt("score"),
                    rs.getBoolean("is_win"),
                    rs.getInt("scored_at"),
                    rs.getInt("game_seconds"),
                )
            }
        }

    /** The players of a line keep their order: the board tells teams apart and names them by it. */
    override fun add(
        boardId: Int,
        entries: List<ScoreEntry>,
    ) = dataSource.inOneTransaction { c ->
        val rowId = BuildPassRowIds.rowOf(c, boardId)
        entries.forEach { entry ->
            val entryId =
                checkNotNull(
                    c.insertForId(ADD) {
                        int(rowId)
                        int(entry.score)
                        boolean(entry.win)
                        int(entry.timestamp)
                        int(entry.seconds)
                    },
                ) { "no key for a highscore entry" }
            c.writeEach(ADD_PLAYER, entry.userIds.withIndex()) { (position, id) ->
                int(entryId)
                int(id)
                int(position)
            }
        }
    }

    override fun dropBefore(
        boardId: Int,
        since: Int,
    ) {
        dataSource.connection.use { c ->
            c.write(DROP_BEFORE) {
                int(BuildPassRowIds.rowOf(c, boardId))
                int(since)
            }
        }
    }

    override fun clear(boardId: Int) {
        dataSource.connection.use { c -> c.write(CLEAR) { int(BuildPassRowIds.rowOf(c, boardId)) } }
    }

    override fun names(ids: Collection<Int>): Map<Int, String> {
        if (ids.isEmpty()) return emptyMap()
        val marks = ids.joinToString(SEPARATOR) { "?" }
        return sql
            .select("SELECT id, name FROM players WHERE id IN ($marks)") { ids.forEach { int(it) } }
            .rows { it.getInt(1) to it.getString(2).orEmpty() }
            .toMap()
    }

    private companion object {
        const val SEPARATOR = ","
        const val ENTRIES =
            "SELECT e.score, e.is_win, e.scored_at, e.game_seconds, " +
                "(SELECT GROUP_CONCAT(p.player_id ORDER BY p.position) " +
                "FROM highscore_entry_players p WHERE p.entry_id = e.id) AS player_ids " +
                "FROM highscore_entries e WHERE e.furni_id = ? AND e.scored_at >= ? ORDER BY e.id"
        const val DROP_BEFORE = "DELETE FROM highscore_entries WHERE furni_id = ? AND scored_at < ?"
        const val ADD =
            "INSERT INTO highscore_entries (furni_id, score, is_win, scored_at, game_seconds) VALUES (?, ?, ?, ?, ?)"
        const val ADD_PLAYER = "INSERT INTO highscore_entry_players (entry_id, player_id, position) VALUES (?, ?, ?)"
        const val CLEAR = "DELETE FROM highscore_entries WHERE furni_id = ?"
    }
}
