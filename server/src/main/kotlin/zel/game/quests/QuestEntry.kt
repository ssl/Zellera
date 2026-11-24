package zel.game.quests

/** A player's row for one quest: followed since [trackedSince] (0 while not followed), [completedAt] 0 until done. */
data class QuestEntry(
    val quest: Int,
    val progress: Int,
    val trackedSince: Int,
    val completedAt: Int,
) {
    constructor(quest: Int) : this(quest, 0, 0, 0)

    val accepted: Boolean get() = trackedSince > 0

    val isCompleted: Boolean get() = completedAt > 0
}
