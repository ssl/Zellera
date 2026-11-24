package zel.game.players.subscriptions

import zel.db.first
import zel.db.write
import javax.sql.DataSource

/** The pay days that were paid out; the next one due is the first pay day after the latest of them. */
class PaydayRounds(
    private val dataSource: DataSource,
) {
    /** Null in a game that never had a pay day. */
    fun latest(): Int? =
        dataSource.connection.use { connection ->
            connection.first(LATEST) { row -> row.getInt(1).takeUnless { row.wasNull() } }
        }

    fun settle(
        roundAt: Int,
        settledAt: Int,
    ) {
        dataSource.connection.use { connection ->
            connection.write(SETTLE) {
                int(roundAt)
                int(settledAt)
            }
        }
    }

    private companion object {
        const val LATEST = "SELECT MAX(round_at) FROM club_payday_rounds"
        const val SETTLE = "INSERT IGNORE INTO club_payday_rounds (round_at, settled_at) VALUES (?, ?)"
    }
}
