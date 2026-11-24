package zel.game.players.inventory

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Database
import zel.db.SqlFailure

class BadgeHolders(
    private val database: Database,
) {
    private val counts = TimedSnapshot<Map<String, Int>>(FRESH_FOR_MS, emptyMap(), RELOAD_GAP_MS, ::count)

    fun count(badgeCode: String): Int = counts.get()[badgeCode] ?: 0

    fun invalidate() = counts.invalidate()

    private fun count(): Map<String, Int>? =
        try {
            database.queries
                .select(COUNT_SQL)
                .rows { it.getString(1) to it.getInt(2) }
                .toMap()
        } catch (failure: SqlFailure) {
            log.error("badge holders not counted", failure)
            null
        }

    private companion object {
        const val FRESH_FOR_MS = 60_000L
        const val RELOAD_GAP_MS = 5_000L
        const val COUNT_SQL = "SELECT badge_code, COUNT(*) FROM player_badges GROUP BY badge_code"
        val log: Logger = LoggerFactory.getLogger(BadgeHolders::class.java)
    }
}
