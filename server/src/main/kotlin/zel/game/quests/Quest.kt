package zel.game.quests

data class Quest(
    val id: Int,
    val code: String,
    val slot: Slot,
    val goal: QuestGoal,
    val reward: QuestReward.Currency,
    val badge: String,
    val isEasy: Boolean,
    val isDaily: Boolean,
    val deadline: Int,
    val display: Display,
) {
    data class Slot(
        val campaign: String,
        val chain: String,
        val position: Int,
    )

    data class Display(
        val catalogPage: String,
        val imageVersion: String,
    )

    /** A quest with a deadline is listed apart and counts down to its campaign's end. */
    val hasDeadline: Boolean get() = deadline > 0

    /** Seconds until the deadline at [now]; none without a deadline, zero once it has passed. */
    fun countdown(now: Int): Int? = if (hasDeadline) (deadline - now).coerceAtLeast(0) else null
}
