package zel.game.players.subscriptions

import org.slf4j.LoggerFactory
import zel.core.rules.rules
import zel.db.allOrNothing
import zel.db.first
import zel.db.rows
import zel.db.write
import zel.game.earnings.IncomeRewards
import zel.game.economy.EconomyLedger
import zel.game.economy.LedgerEntry
import zel.game.economy.LedgerTransaction
import zel.game.players.Player
import zel.game.players.PlayerProfile
import zel.game.players.PlayerStats
import zel.game.players.WalletWrites
import zel.game.players.required
import zel.protocol.generated.ActivityPointNotificationOut
import zel.protocol.generated.ActivityPointNotificationOutCodec
import zel.server.GameServices
import zel.util.GameZone
import zel.util.SECONDS_PER_MINUTE
import java.sql.Connection
import java.sql.SQLException
import java.sql.SQLIntegrityConstraintViolationException
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

object ClubPayday {
    private val log = LoggerFactory.getLogger(ClubPayday::class.java)

    private const val MESSAGE_KEY = "club.payday.paid"
    private const val MESSAGE_FALLBACK = "It's club payday! %amount% has been paid into your purse."
    private const val LEDGER_CAUSE = "club.payday"
    private const val MEMBERS_AT =
        "SELECT DISTINCT player_id FROM player_subscriptions WHERE kind = '${Subscription.KIND_CLUB}' AND active " +
            "AND started_at <= ? AND started_at + duration_seconds > ?"
    private const val LOG_ROUND =
        "INSERT INTO club_paydays (player_id, round_at, paid_at, streak_days, spent, streak_reward, " +
            "spending_reward, currency) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
    private val DAY_FORMAT: DateTimeFormatter
        get() = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ROOT).withZone(GameZone.current)

    /** Credit furni is left out: credits packed into furni are moved, not spent. */
    internal const val SPENT =
        "SELECT COALESCE(SUM(price_credits), 0) FROM purchases WHERE player_id = ? AND bought_at > ? " +
            "AND bought_at <= ? AND NOT EXISTS (SELECT 1 FROM catalogue_offer_items item JOIN furni_definitions " +
            "furni ON furni.id = item.definition_id WHERE item.offer_id = purchases.offer_id " +
            "AND (furni.code LIKE 'CF\\_%' OR furni.code LIKE 'CFC\\_%'))"

    /**
     * What the next pay day brings as things stand. The streak is the whole days of the oldest membership still
     * running: an extension keeps it, a lapse ends it. Spending counts up to [spentUntil].
     */
    fun forecast(
        services: GameServices,
        profile: PlayerProfile,
        stats: PlayerStats = profile.loadStats(),
        spentUntil: Int = services.unixTimestamp(),
    ): PaydayForecast {
        val settings = ClubSettings.payday
        val now = services.unixTimestamp()
        val memberships = stats.club.subscriptions.filter { it.type.equals(Subscription.CLUB, true) }
        val running = memberships.filter { it.isActive }.minByOrNull { it.startedAt }
        if (running == null || !rules.clubPayday) {
            return PaydayForecast(
                streakDays = 0,
                firstSubscribed = firstDay(memberships),
                kickbackShare = settings.kickbackShare,
                spentSincePayday = 0,
                streakReward = 0,
                spendingReward = 0,
                minutesUntilPayday = 0,
            )
        }
        val streakDays = PaydaySchedule.wholeDays(now - running.startedAt)
        val spent = spentBetween(services, profile.id, stats.club.lastPayday, spentUntil)
        val next = PaydaySchedule.firstAfter(now, settings.dayOfMonth, GameZone.current)
        return PaydayForecast(
            streakDays = streakDays,
            firstSubscribed = firstDay(memberships),
            kickbackShare = settings.kickbackShare,
            spentSincePayday = spent,
            streakReward = settings.streakReward(streakDays),
            spendingReward = settings.kickback(spent),
            minutesUntilPayday = (next - now) / SECONDS_PER_MINUTE,
        )
    }

    /**
     * Pays the round that fell due at [dueAt]; false when it could not run and has to run again. A member's log
     * row is the claim on the round and is written with the payment, so a round that runs again pays nobody twice.
     */
    fun run(
        services: GameServices,
        dueAt: Int,
    ): Boolean {
        val now = services.unixTimestamp()
        val currency =
            ClubSettings.payday.currency
                .trim()
                .toIntOrNull()
                ?.takeIf { it >= EconomyLedger.CREDITS }
        if (currency == null) log.error("club.payday.currency is not a currency code; this pay day pays nothing")
        return try {
            val members =
                services.database.dataSource.connection
                    .use { membersAt(it, now) }
            if (currency != null) members.forEach { payMember(services, Round(dueAt, now, currency), it) }
            true
        } catch (e: SQLException) {
            log.error("the pay day did not run", e)
            false
        }
    }

    internal class Round(
        val dueAt: Int,
        val paidAt: Int,
        val currency: Int,
    )

    @Suppress("TooGenericExceptionCaught") // one member failing in any way must not stop the others
    private fun payMember(
        services: GameServices,
        round: Round,
        playerId: Int,
    ) {
        try {
            val players = services.game.players.required()
            val profile = players.profileOf(playerId) ?: return
            val stats = profile.loadStats()
            val forecast = forecast(services, profile, stats, spentUntil = round.dueAt)
            if (forecast.total > 0) hand(services, round, players.find(playerId), playerId, forecast)
            // the next round counts spending from this round's due moment, however late the round ran
            stats.club.lastPayday = maxOf(stats.club.lastPayday, round.dueAt)
            services.threading.run(stats)
        } catch (e: Exception) {
            log.error("the pay day of player {} failed", playerId, e)
        }
    }

    /** Credits and points wait in the earnings centre; other currencies go straight to the purse. */
    private fun hand(
        services: GameServices,
        round: Round,
        online: Player?,
        playerId: Int,
        forecast: PaydayForecast,
    ) {
        val earnings = earningsKind(round.currency)
        val settle = {
            services.database.dataSource.connection.use { connection ->
                connection.allOrNothing { settle(connection, round, playerId, forecast, earnings) }
            }
        }
        if (earnings != null) {
            if (settle() != null) services.game.incomeRewards?.notifyNew(playerId, IncomeRewards.CLUB)
            return
        }
        if (online == null) {
            settle()
            return
        }
        val balance =
            WalletWrites.locked(online) {
                settle()?.also { WalletWrites.publish(online, round.currency, it) }
            } ?: return
        online.client?.send(
            ActivityPointNotificationOutCodec,
            ActivityPointNotificationOut(balance, forecast.total, round.currency),
        )
        online.alert(services.texts.text(MESSAGE_KEY, MESSAGE_FALLBACK).replace("%amount%", "${forecast.total}"))
    }

    /** The balance the payment left, 0 for one that waits in the earnings centre; null for a round paid before. */
    internal fun settle(
        connection: Connection,
        round: Round,
        playerId: Int,
        forecast: PaydayForecast,
        earnings: Int?,
    ): Int? {
        try {
            connection.write(LOG_ROUND) {
                int(playerId)
                int(round.dueAt)
                int(round.paidAt)
                int(forecast.streakDays)
                int(forecast.spentSincePayday)
                int(forecast.streakReward)
                int(forecast.spendingReward)
                int(round.currency)
            }
        } catch (_: SQLIntegrityConstraintViolationException) {
            return null
        }
        if (earnings != null) {
            IncomeRewards.insert(connection, playerId, IncomeRewards.CLUB, earnings, forecast.total, "", round.paidAt)
            return 0
        }
        val key = "payday/${round.dueAt}/$playerId"
        return LedgerTransaction(connection)
            .post(LedgerEntry(key, playerId, round.currency, forecast.total, LEDGER_CAUSE))
            .after
    }

    fun earningsKind(currency: Int): Int? =
        when (currency) {
            EconomyLedger.CREDITS -> IncomeRewards.CREDITS
            EconomyLedger.POINTS -> IncomeRewards.POINTS
            else -> null
        }

    private fun spentBetween(
        services: GameServices,
        playerId: Int,
        after: Int,
        until: Int,
    ): Int =
        try {
            services.database.dataSource.connection.use { connection ->
                connection.first(SPENT, {
                    int(playerId)
                    int(after)
                    int(until)
                }) { it.getInt(1) } ?: 0
            }
        } catch (e: SQLException) {
            log.error("the spending of player {} was not read", playerId, e)
            0
        }

    private fun membersAt(
        connection: Connection,
        moment: Int,
    ): List<Int> =
        connection.rows(MEMBERS_AT, {
            int(moment)
            int(moment)
        }) { it.getInt("player_id") }

    private fun firstDay(memberships: List<Subscription>): String =
        memberships
            .minOfOrNull { it.startedAt }
            ?.let { DAY_FORMAT.format(Instant.ofEpochSecond(it.toLong())) }
            .orEmpty()
}
