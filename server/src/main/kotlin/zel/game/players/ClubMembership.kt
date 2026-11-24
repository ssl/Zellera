package zel.game.players

import zel.game.players.subscriptions.Subscription
import zel.server.GameServices
import zel.util.SECONDS_PER_DAY
import java.sql.ResultSet

/** A club month: the length of one membership period and of one club gift. */
const val CLUB_MONTH_DAYS = 31

private const val SECONDS_PER_CLUB_GIFT = CLUB_MONTH_DAYS * SECONDS_PER_DAY
private const val NO_GIFTS = -1

class ClubMembership internal constructor(
    private val services: GameServices,
    private val userId: Int,
    private val monitor: Any,
    private val ledgerLock: () -> Any,
) {
    /** Replaced as a whole under the locks of [create], read without a lock. */
    @Volatile
    var subscriptions: Set<Subscription> = emptySet()
        internal set

    var lastPayday = 0
    var giftsClaimed = 0
    var buildPassBonusFurni = 0

    fun get(type: String): Subscription? =
        subscriptions.firstOrNull {
            it.type.equals(type, ignoreCase = true) && it.isActive &&
                it.secondsLeft > 0
        }

    fun has(type: String): Boolean = get(type) != null

    fun expireTimestamp(type: String): Int = get(type)?.endsAt ?: 0

    val isMember: Boolean get() = has(Subscription.CLUB)

    val clubExpiresAt: Int get() = expireTimestamp(Subscription.CLUB)

    fun create(
        type: String,
        duration: Int,
        onRecorded: () -> Unit = {},
    ): Subscription? =
        synchronized(ledgerLock()) { synchronized(monitor) { createCoordinated(type, duration, onRecorded) } }

    val pastClubTime: Int
        get() = pastClubTime(subscriptions)

    /** Counts the running day; 0 if never a member. */
    val pastClubDays: Int get() = pastClubDays(subscriptions)

    val totalClubDays: Int get() = totalClubDays(subscriptions)

    val timeTillNextGift: Int
        get() {
            val past = pastClubTime
            return giftsOwed(past) * SECONDS_PER_CLUB_GIFT - past
        }

    /** -1 for a player who never had club. */
    val daysTillNextGift: Int get() = if (totalClubDays > 0) timeTillNextGift / SECONDS_PER_DAY else NO_GIFTS

    val remainingGifts: Int get() = giftsOwed(pastClubTime) - giftsClaimed

    private fun giftsOwed(pastTime: Int): Int = if (totalClubDays > 0) pastTime / SECONDS_PER_CLUB_GIFT + 1 else 0

    /** Adds [amount] bonus furni; false, adding nothing, when the bonus would leave the Int range. */
    fun addBuildPassBonusFurni(amount: Int): Boolean {
        if (amount <= 0) return true
        val total = buildPassBonusFurni.toLong() + amount
        if (total > Int.MAX_VALUE) return false
        buildPassBonusFurni = total.toInt()
        return true
    }

    internal fun read(row: ResultSet) {
        lastPayday = row.getInt("last_club_payday_at")
        giftsClaimed = row.getInt("club_gifts_claimed")
        buildPassBonusFurni = row.getInt("build_pass_bonus_furni")
    }

    private fun createCoordinated(
        type: String,
        duration: Int,
        onRecorded: () -> Unit,
    ): Subscription? {
        val running = get(type) ?: return open(type, duration, onRecorded)
        if (!endsInRange(running.endsAt, duration)) return null
        running.extend(duration, onRecorded)
        return running
    }

    private fun open(
        type: String,
        duration: Int,
        onRecorded: () -> Unit,
    ): Subscription? {
        val now = services.unixTimestamp()
        if (!endsInRange(now, duration)) return null
        val created =
            services.game.subscriptions
                .required()
                .open(userId, type, now, duration) ?: return null
        subscriptions = subscriptions + created
        onRecorded()
        created.started()
        return created
    }

    companion object {
        private fun endsInRange(
            end: Int,
            seconds: Int,
        ): Boolean = end.toLong() + seconds <= Int.MAX_VALUE

        private fun clubOf(subscriptions: Iterable<Subscription>) =
            subscriptions.filter { it.type.equals(Subscription.CLUB, ignoreCase = true) }

        private fun pastClubTime(subscriptions: Iterable<Subscription>): Int =
            clubOf(subscriptions).sumOf { it.duration - maxOf(it.secondsLeft, 0) }

        internal fun pastClubDays(subscriptions: Iterable<Subscription>): Int =
            if (totalClubDays(subscriptions) > 0) pastClubTime(subscriptions) / SECONDS_PER_DAY + 1 else 0

        internal fun totalClubDays(subscriptions: Iterable<Subscription>): Int =
            (clubOf(subscriptions).sumOf { maxOf(it.duration, 0).toLong() } / SECONDS_PER_DAY)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
    }
}
