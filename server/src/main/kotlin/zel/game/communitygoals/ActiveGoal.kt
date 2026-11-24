package zel.game.communitygoals

/** The goal the landing view shows: the one that opened last. */
data class ActiveGoal(
    val id: Int,
    val code: String,
    val levels: List<Level>,
    val closesAt: Int,
) {
    data class Level(
        val threshold: Int,
        val rewardedPlayers: Int?,
    )

    data class Reach(
        val level: Int,
        val missing: Int,
        val percent: Int,
    )

    val rewardLimits: List<Int> get() = levels.mapNotNull { it.rewardedPlayers }

    fun reach(total: Int): Reach {
        val reached = levels.count { total >= it.threshold }
        val next = levels.getOrNull(reached)?.threshold ?: return Reach(reached, 0, FULL)
        val previous = levels.getOrNull(reached - 1)?.threshold ?: 0
        val percent = if (next <= previous) FULL else ((total - previous).toLong() * FULL / (next - previous)).toInt()
        return Reach(reached, maxOf(0, next - total), percent.coerceIn(0, FULL))
    }

    fun isOver(now: Int): Boolean = closesAt in 1..now

    fun secondsLeft(now: Int): Int = if (closesAt > 0) maxOf(0, closesAt - now) else 0

    private companion object {
        const val FULL = 100
    }
}
