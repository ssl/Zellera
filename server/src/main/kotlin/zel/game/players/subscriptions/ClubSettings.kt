package zel.game.players.subscriptions

import java.util.SortedMap
import java.util.TreeMap

/**
 * Each group is replaced whole when one of its `settings` rows changes.
 * Its settings are @JvmField: an object property's setter would write a static field (SpotBugs ST).
 */
object ClubSettings {
    data class Payday(
        val dayOfMonth: Int = PaydaySchedule.DAYS.first,
        val currency: String = "",
        val kickbackPercent: Int = DEFAULT_KICKBACK_PERCENT,
        val streakRewards: SortedMap<Int, Int> = sortedMapOf(),
        /** A new membership counts spending from its start instead of from the start of the pay period. */
        val spendingFromStart: Boolean = false,
    ) {
        val kickbackShare: Double get() = kickbackPercent / PERCENT

        /** The reward of the longest streak length reached; a streak shorter than every length earns nothing. */
        fun streakReward(days: Int): Int =
            streakRewards
                .headMap(days + 1)
                .values
                .maxOrNull()
                ?.coerceAtLeast(0) ?: 0

        /** The share of [spent] in whole units, cut. */
        fun kickback(spent: Int): Int {
            val hundredths = maxOf(0, spent).toLong() * maxOf(0, kickbackPercent)
            return (hundredths / HUNDRED).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        }
    }

    data class Discount(
        val enabled: Boolean = false,
        val daysBeforeEnd: Int = DEFAULT_DISCOUNT_DAYS,
    ) {
        fun covers(daysLeft: Int): Boolean = enabled && daysLeft <= daysBeforeEnd
    }

    @JvmField
    @Volatile
    var payday = Payday()

    @JvmField
    @Volatile
    var discount = Discount()

    const val DEFAULT_KICKBACK_PERCENT = 10
    private const val PERCENT = 100.0
    private const val HUNDRED = 100L
    const val DEFAULT_DISCOUNT_DAYS = 7
    const val DEFAULT_STREAK_REWARDS = "7=5;30=10;60=15;90=20;180=25;365=30"

    /** `days=reward` pairs separated by `;`; a pair that is not two whole numbers refuses the whole text. */
    fun streakRewards(text: String): SortedMap<Int, Int> {
        val rewards = TreeMap<Int, Int>()
        text.split(';').map(String::trim).filter(String::isNotEmpty).forEach { pair ->
            val days = pair.substringBefore('=').trim().toIntOrNull()
            val reward = pair.substringAfter('=', "").trim().toIntOrNull()
            require(days != null && reward != null && days >= 0 && reward >= 0) { "not days=reward: $pair" }
            rewards[days] = reward
        }
        return rewards
    }
}
