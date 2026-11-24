package zel.game.players.subscriptions

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.core.rules.rules
import zel.game.players.Player
import zel.game.players.WalletWrites
import zel.server.GameServices
import zel.util.GameZone
import java.sql.SQLException
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class MembershipWatch(
    private val services: GameServices,
    private val rounds: PaydayRounds = PaydayRounds(services.database.dataSource),
    private val payday: (GameServices, Int) -> Boolean = ClubPayday::run,
) {
    private val paying = AtomicBoolean()

    /** The latest pay day paid out; read and written only while [paying] is held. */
    private var settledRound: Int? = null

    fun period(): Duration = Duration.ofSeconds(services.config.getInt(INTERVAL_KEY, DEFAULT_SECONDS).toLong())

    fun check() {
        services.game.players
            ?.all
            ?.forEach(::endRunOutMemberships)
        payIfDue()
    }

    /** Under the wallet and stats locks a purchase extends under, so a membership being extended is not ended. */
    @Suppress("TooGenericExceptionCaught") // one player's failure must not stop the others or the pay day
    private fun endRunOutMemberships(player: Player) {
        try {
            WalletWrites.locked(player) {
                synchronized(player.stats) {
                    player.stats.club.subscriptions
                        .filter { it.isActive && it.secondsLeft <= 0 }
                        .forEach { it.expire() }
                }
            }
        } catch (failure: Exception) {
            LOG.error("memberships of player {} not ended", player.profile.id, failure)
        }
    }

    /**
     * The round due is the latest pay day that has come. A gap of several months is one round: every member's
     * spending is counted from their own last pay day, so nothing is lost by paying it once.
     */
    private fun payIfDue() {
        if (!rules.clubPayday || !paying.compareAndSet(false, true)) return
        try {
            val now = services.unixTimestamp()
            val due = PaydaySchedule.latestAtOrBefore(now, ClubSettings.payday.dayOfMonth, GameZone.current)
            val settled = settledRound ?: rounds.latest() ?: opening(due, now)
            if (due > settled) {
                if (!payday(services, due)) return
                rounds.settle(due, services.unixTimestamp())
            }
            settledRound = maxOf(due, settled)
        } catch (failure: SQLException) {
            LOG.error("the pay day rounds were not read or written", failure)
        } finally {
            paying.set(false)
        }
    }

    /** A game's first look finds no round: the pay day already past counts as settled, the next is the first paid. */
    private fun opening(
        due: Int,
        now: Int,
    ): Int {
        rounds.settle(due, now)
        return due
    }

    private companion object {
        val LOG: Logger = LoggerFactory.getLogger(MembershipWatch::class.java)
        const val INTERVAL_KEY = "club.expiry_check.interval.seconds"
        const val DEFAULT_SECONDS = 10
    }
}
