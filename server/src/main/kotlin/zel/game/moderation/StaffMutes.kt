package zel.game.moderation

import org.slf4j.LoggerFactory
import zel.core.Configuration
import zel.db.allOrNothing
import zel.db.first
import zel.db.write
import zel.game.permissions.Privilege
import zel.game.players.Players
import zel.util.SECONDS_PER_HOUR
import java.sql.SQLException
import javax.sql.DataSource

/** Mutes given by staff, to a player online or not; no single one is longer than the setting allows. */
class StaffMutes(
    private val dataSource: DataSource,
    private val players: Players,
    private val config: Configuration,
    private val clock: () -> Int,
) {
    val capHours: Int get() = maxOf(1, config.getInt(CAP_KEY, DEFAULT_CAP_HOURS))

    /** The seconds [playerId] was muted for, 0 when the player is unknown or never muted. */
    fun give(
        playerId: Int,
        seconds: Int,
    ): Int {
        val length = seconds.coerceAtMost(capHours * SECONDS_PER_HOUR)
        if (length <= 0) return 0
        val online = players.find(playerId)
        val muted =
            if (online != null) {
                online.mute(length)
            } else {
                players.readProfile(playerId)?.rank?.allows(Privilege.CHAT_MUTE_IMMUNE.key) == false &&
                    store(playerId, length)
            }
        return if (muted) length else 0
    }

    private fun store(
        playerId: Int,
        seconds: Int,
    ): Boolean =
        try {
            dataSource.connection.use { connection ->
                connection.allOrNothing {
                    val running =
                        connection.first(READ, { int(playerId) }) { it.getLong("muted_until") }
                            ?: return@allOrNothing null
                    connection.write(WRITE) {
                        long(lengthened(running, clock(), seconds))
                        int(playerId)
                    }
                } == 1
            }
        } catch (e: SQLException) {
            log.error("mute of {} not stored", playerId, e)
            false
        }

    companion object {
        const val CAP_KEY = "moderation.mute.max.hours"
        const val DEFAULT_CAP_HOURS = 168

        /** What the mod tool's own mute button gives: the one length the client offers. */
        const val TOOL_SECONDS = SECONDS_PER_HOUR

        private val log = LoggerFactory.getLogger(StaffMutes::class.java)
        private const val READ =
            "SELECT COALESCE(muted_until, 0) AS muted_until FROM player_standing WHERE player_id = ? FOR UPDATE"
        private const val WRITE = "UPDATE player_standing SET muted_until = ? WHERE player_id = ?"

        /** A running mute goes on for [seconds] more, an ended one starts at [now]. */
        fun lengthened(
            mutedUntil: Long,
            now: Int,
            seconds: Int,
        ): Long = maxOf(mutedUntil, now.toLong()) + seconds
    }
}
