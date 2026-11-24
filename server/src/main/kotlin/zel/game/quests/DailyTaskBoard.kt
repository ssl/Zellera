package zel.game.quests
import zel.game.players.PlayerStates
import java.time.LocalDate

/** One player's daily task progress: the rows of [day] and the completed or claimed rows of the days before it. */
internal class DailyTaskBoard(
    val playerId: Int,
    val day: LocalDate,
) : PlayerStates.Held() {
    private val today = HashMap<Int, DailyTaskProgress>()
    private val earlier = HashMap<Pair<Int, LocalDate>, DailyTaskProgress>()

    operator fun get(taskId: Int): DailyTaskProgress? = today[taskId]

    fun restore(progress: DailyTaskProgress) {
        today[progress.taskId] = progress
    }

    fun restoreEarlier(progress: DailyTaskProgress) {
        earlier[progress.taskId to progress.day] = progress
    }

    /** Still claimable from the days before [day], the newest day first. */
    fun earlierClaimable(): List<DailyTaskProgress> =
        earlier.values.filter { it.day < day && it.isClaimable }.sortedByDescending { it.day }

    fun isDone(taskId: Int): Boolean = today[taskId]?.isOpen == false

    /** Counts [amount] on [task]; null once the task is complete. */
    fun count(
        task: DailyTask,
        amount: Int,
    ): DailyTaskProgress? {
        val current = today[task.id] ?: DailyTaskProgress(task.id, day, 0, DailyTaskStatus.OPEN)
        if (!current.isOpen) return null
        val repeats = minOf(task.requiredRepeats, current.repeats + amount)
        val status = if (repeats >= task.requiredRepeats) DailyTaskStatus.COMPLETED else DailyTaskStatus.OPEN
        return DailyTaskProgress(task.id, day, repeats, status).also { today[task.id] = it }
    }

    /** The claimed row of [taskId] on [onDay], or null when nothing of that day waits for a claim. */
    fun claim(
        taskId: Int,
        onDay: LocalDate,
    ): DailyTaskProgress? {
        if (onDay == day) {
            val current = today[taskId]?.takeIf { it.isClaimable } ?: return null
            return current.copy(status = DailyTaskStatus.CLAIMED).also { today[taskId] = it }
        }
        val key = taskId to onDay
        val current = earlier[key]?.takeIf { it.day < day && it.isClaimable } ?: return null
        return current.copy(status = DailyTaskStatus.CLAIMED).also { earlier[key] = it }
    }

    /** An older day's board wins for completed and claimed rows, since its claim writes may still be queued. */
    fun takeOverFrom(previous: DailyTaskBoard) {
        (previous.earlier.values + previous.today.values)
            .filterNot { it.isOpen }
            .forEach(::restoreEarlier)
    }
}
