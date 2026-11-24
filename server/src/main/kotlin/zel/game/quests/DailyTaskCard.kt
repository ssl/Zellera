package zel.game.quests

data class DailyTaskCard(
    val listedId: Long,
    val task: DailyTask,
    val repeats: Int,
    val status: DailyTaskStatus,
    val secondsLeft: Int,
) {
    val done: Int get() = minOf(repeats, task.requiredRepeats)
}
