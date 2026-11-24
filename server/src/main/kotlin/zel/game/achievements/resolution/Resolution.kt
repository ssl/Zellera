package zel.game.achievements.resolution

/** A promise to reach [level] of an achievement; an [endsAt] of 0 means no deadline. */
data class Resolution(
    val furniId: Int,
    val playerId: Int,
    val achievement: Int,
    val level: Int,
    val endsAt: Int,
    val completedAt: Int = 0,
) {
    val isCompleted: Boolean get() = completedAt > 0

    fun remaining(now: Int): Int = (endsAt - now).coerceAtLeast(0)

    fun isExpired(now: Int): Boolean = !isCompleted && endsAt in 1..now
}
