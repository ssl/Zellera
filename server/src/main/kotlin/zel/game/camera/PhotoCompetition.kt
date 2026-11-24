package zel.game.camera

import zel.core.Configuration
import zel.db.Sql
import zel.db.SqlFailure
import zel.db.noneAsNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Each submission stays as a row; the newest of a player is the entry that stands. */
class PhotoCompetition(
    private val config: Configuration,
    private val sql: Sql,
    private val now: () -> Int,
) {
    enum class Door { CLOSED, DAILY_LIMIT_REACHED, OPEN }

    fun door(playerId: Int): Door =
        when {
            !config.getBoolean("camera.competition.is_open", false) -> Door.CLOSED
            submittedToday(playerId) >= dailyLimit() -> Door.DAILY_LIMIT_REACHED
            else -> Door.OPEN
        }

    /** Stores [photoId] in the entry's `url` column. */
    fun submit(
        playerId: Int,
        roomId: Int,
        photoId: String,
    ): Boolean =
        try {
            sql.write(ADD, playerId, noneAsNull(roomId), photoId, now())
            true
        } catch (failure: SqlFailure) {
            LOGGER.error("Could not store a competition photo", failure)
            false
        }

    private fun dailyLimit() = maxOf(1, config.getInt("camera.competition.entries_per_day", DEFAULT_DAILY_LIMIT))

    private fun submittedToday(playerId: Int): Int =
        try {
            sql.select(COUNT_SINCE, playerId, now() - DAY_SECONDS).first { it.getInt(1) } ?: 0
        } catch (failure: SqlFailure) {
            LOGGER.error("Could not count competition photos", failure)
            Int.MAX_VALUE
        }

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(PhotoCompetition::class.java)
        const val DAY_SECONDS = 86400
        const val DEFAULT_DAILY_LIMIT = 3
        const val COUNT_SINCE =
            "SELECT COUNT(*) FROM photo_competition_entries WHERE player_id = ? AND submitted_at > ?"
        const val ADD =
            "INSERT INTO photo_competition_entries (player_id, room_id, url, submitted_at) VALUES (?, ?, ?, ?)"
    }
}
