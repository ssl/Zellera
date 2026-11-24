package zel.core

import zel.core.rules.rules
import zel.core.threading.GameThreads
import zel.db.write
import zel.game.Game
import zel.protocol.outgoing.users.UserData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException
import java.time.Duration
import java.time.Instant
import javax.sql.DataSource

class Housekeeping(
    private val config: Configuration,
    private val dataSource: DataSource?,
    private val commandLog: CommandLog,
    private val threading: GameThreads,
    private val state: ServerState,
    private val game: () -> Game?,
    marks: ChoreMarks = JdbcChoreMarks(dataSource),
) {
    private val respectRefill = DailyRefill(marks, DailyRound.of(config), refill = ::refillDailyRespects)
    private val running = mutableListOf<Recurring>()

    /** Every chore has a timer of its own; a slow one holds up no other. */
    @Synchronized
    fun start() {
        repairAfterLastRun()
        every(COMMAND_LOG) { commandLog.write() }
        gameChore(IDLE_GUILDS) { game ->
            game.guilds?.let { guilds ->
                guilds.forgetIdle()
                guilds.forumThreads.flush()
            }
        }
        gameChore(PURCHASE_LOGS, ::prunePurchaseLogs)
        gameChore(HALL_OF_FAME) { it.landing?.hallOfFame?.reload() }
        gameChore(NEWS) { it.landing?.news?.refresh() }
        every(EXPIRED_SWEEP, first = Duration.ZERO, ::sweepExpired)
        val refill = Recurring(threading, respectRefill::untilNextRound, work = respectRefill::runIfDue)
        running += refill.also { it.start(first = respectRefill.untilNextRound()) }
        OnlineMinutes(threading, state, game).start()
    }

    @Synchronized
    fun stop() {
        running.forEach(Recurring::end)
        running.clear()
    }

    private fun every(
        period: Duration,
        first: Duration = period,
        work: () -> Unit,
    ) {
        running += Recurring(threading, { period }, work = work).also { it.start(first) }
    }

    private fun gameChore(
        period: Duration,
        work: (Game) -> Unit,
    ) = every(period) { game()?.let(work) }

    /**
     * Nobody is online and no room is occupied before the first login, whatever the last run left behind; a daily
     * round that began while the server was down gets its refill before anyone logs in.
     */
    internal fun repairAfterLastRun() {
        try {
            dataSource?.connection?.use { connection ->
                connection.write("UPDATE rooms SET player_count = 0 WHERE player_count > 0")
                connection.write("UPDATE players SET is_online = FALSE WHERE is_online = TRUE")
                settleLapsedEffects(connection, now())
            }
            log.info("cleared what the last run left behind")
        } catch (e: SQLException) {
            log.error("clearing what the last run left behind failed", e)
        }
        respectRefill.runIfDue()
    }

    /** A timed effect that ran out while nobody watched loses the copy that ran; one with no copy left goes. */
    private fun settleLapsedEffects(
        connection: Connection,
        now: Int,
    ) {
        connection.write(SPEND_LAPSED_EFFECTS) { int(now) }
        connection.write("DELETE FROM player_effects WHERE remaining <= 0")
    }

    /** Readers judge a ban or mute by its end themselves; the sweep only keeps the tables from growing. */
    internal fun sweepExpired() {
        val now = now()
        try {
            dataSource?.connection?.use { connection ->
                connection.write("DELETE FROM room_bans WHERE ends_at < ?") { int(now) }
                connection.write("DELETE FROM room_mutes WHERE ends_at < ?") { int(now) }
            }
        } catch (e: SQLException) {
            log.error("expired room bans and mutes not swept", e)
        }
    }

    private fun refillDailyRespects() {
        val respects = rules.dailyRespects
        val petRespects = rules.dailyPetRespects
        val refills = config.getInt("respect.buybacks.per_day", 1)
        try {
            dataSource?.connection?.use { connection ->
                connection.write(REFILL_RESPECTS) {
                    int(respects)
                    int(petRespects)
                    int(refills)
                }
            }
        } catch (e: SQLException) {
            log.error("daily respects not refilled", e)
        }

        if (!state.isReady) return
        game()?.players?.all?.forEach { player ->
            player.stats.tally.respectsLeft = respects
            player.stats.tally.petRespectsLeft = petRespects
            player.stats.tally.respectRefillsLeft = refills
            player.client?.send(UserData(player))
        }
    }

    // One player's failure must not stop the others.
    @Suppress("TooGenericExceptionCaught")
    private fun prunePurchaseLogs(game: Game) {
        game.players?.all?.forEach { player ->
            try {
                player.stats.limitedPurchases.prune()
            } catch (e: Exception) {
                log.error("limited purchase log of player {} not pruned", player.profile.id, e)
            }
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(Housekeeping::class.java)

        val COMMAND_LOG: Duration = Duration.ofSeconds(30)
        val IDLE_GUILDS: Duration = Duration.ofMinutes(2)
        val HALL_OF_FAME: Duration = Duration.ofMinutes(15)
        val PURCHASE_LOGS: Duration = Duration.ofMinutes(30)
        val NEWS: Duration = Duration.ofMinutes(30)
        val EXPIRED_SWEEP: Duration = Duration.ofHours(1)

        const val SPEND_LAPSED_EFFECTS =
            "UPDATE player_effects SET remaining = remaining - 1, activated_at = NULL WHERE duration_seconds > 0 " +
                "AND activated_at + duration_seconds < ?"
        const val REFILL_RESPECTS =
            "UPDATE player_stats SET daily_respect_left = ?, daily_pet_respect_left = ?, " +
                "daily_respect_refills_left = ?"

        fun now(): Int = Instant.now().epochSecond.toInt()
    }
}
