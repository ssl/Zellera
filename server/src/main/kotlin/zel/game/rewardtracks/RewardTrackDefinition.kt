package zel.game.rewardtracks

import zel.game.quests.CountedAction

data class RewardTrackDefinition(
    val id: String,
    val theme: String,
    val tasks: List<Task>,
    val prizes: List<Prize>,
) {
    data class Level(
        val requiredCount: Int,
        val points: Int,
        val premium: Boolean,
    )

    /** [goal] is null for actions nothing reports, which never progress. */
    data class Task(
        val id: String,
        val actionType: String,
        val parameter: String,
        val premium: Boolean,
        val goal: CountedAction?,
        val levels: List<Level>,
    ) {
        fun lastCount(): Int = levels.maxOfOrNull { it.requiredCount }?.coerceAtLeast(0) ?: 0

        fun pointsBetween(
            before: Int,
            after: Int,
        ): Int = levels.filter { it.requiredCount > before && it.requiredCount <= after }.sumOf { it.points }
    }

    /** A prize; [productType]: 1 floor furni, 0 wall furni, 4 badge, 6 bot, 8 activity points, 10 pet, 12 icon. */
    data class Prize(
        val id: String,
        val requiredPoints: Int,
        val productType: Int,
        val productCode: String,
        val extra: String,
        val amount: Int,
        val premium: Boolean,
    )

    fun prize(prizeId: String): Prize? = prizes.firstOrNull { it.id == prizeId }

    /** A track without free prizes never completes. */
    fun completedBy(claimed: Set<String>): Boolean {
        val free = prizes.filterNot { it.premium }
        return free.isNotEmpty() && free.all { it.id in claimed }
    }
}
