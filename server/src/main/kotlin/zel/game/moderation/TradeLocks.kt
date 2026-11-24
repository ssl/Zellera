// SQL parameter positions and minutes to seconds are plain numbers here.
@file:Suppress("MagicNumber")

package zel.game.moderation

import org.slf4j.LoggerFactory
import zel.core.Configuration
import zel.db.write
import zel.game.permissions.Privilege
import zel.game.players.Player
import zel.util.MINUTES_PER_HOUR
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

class TradeLocks(
    private val dataSource: DataSource,
    private val standing: StandingCounters,
    private val config: Configuration = Configuration(emptyMap()),
) {
    /**
     * The minutes a lock [moderator] asks for may run, null when they may not give it: past the plain limit a lock
     * is as heavy as a ban and takes the ban right, and nothing runs longer than the tool's own "permanent".
     */
    fun grantedMinutes(
        moderator: Player,
        minutes: Int,
    ): Int? {
        val plainLimit = maxOf(1, config.getInt(PLAIN_LIMIT_KEY, DEFAULT_PLAIN_LIMIT_HOURS)).toLong() * MINUTES_PER_HOUR
        return when {
            minutes <= 0 -> null
            minutes <= plainLimit -> minutes
            moderator.hasPermission(Privilege.MODERATION_PLAYER_BAN) -> minOf(minutes, PERMANENT_MINUTES)
            else -> null
        }
    }

    fun lock(
        player: Player,
        until: Int,
    ) {
        player.stats.standing.lockTrade(until)

        try {
            dataSource.connection.use { store(it, player.profile.id, until) }
        } catch (e: SQLException) {
            log.error("trade lock of {} not stored", player.profile.id, e)
        }

        player.rooms.stopTrade(player.profile.id)
        standing.count(player.profile.id, StandingCounter.TRADE_LOCKS)
    }

    fun lockOffline(
        userId: Int,
        until: Int,
    ): Boolean =
        try {
            dataSource.connection.use { store(it, userId, until) }
            standing.count(userId, StandingCounter.TRADE_LOCKS)
            true
        } catch (e: SQLException) {
            log.error("trade lock of {} not stored", userId, e)
            false
        }

    companion object {
        const val PLAIN_LIMIT_KEY = "moderation.trade_lock.max.hours"
        const val DEFAULT_PLAIN_LIMIT_HOURS = 168

        /** The mod tool's "permanent" lock: 876000 hours. */
        const val PERMANENT_MINUTES = 876_000 * MINUTES_PER_HOUR

        private val log = LoggerFactory.getLogger(TradeLocks::class.java)

        /** When a lock of [minutes] given at [now] ends (unix time), 0 for no length; capped at the last int second. */
        fun until(
            now: Int,
            minutes: Int,
        ): Int = if (minutes <= 0) 0 else minOf(now.toLong() + minutes * 60L, Int.MAX_VALUE.toLong()).toInt()

        internal fun store(
            connection: Connection,
            userId: Int,
            until: Int,
        ) {
            connection.write(
                "UPDATE player_standing SET trade_locked_until = ? " +
                    "WHERE player_id = ? AND (trade_locked_until IS NULL OR trade_locked_until < ?)",
            ) {
                int(until)
                int(userId)
                int(until)
            }
        }
    }
}
