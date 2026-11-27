package zel.game.quests

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.Random

internal class DailyTaskRoster(
    private val tasksPerDay: () -> Int,
) {
    @Volatile
    private var enabled: Map<Int, DailyTask> = emptyMap()

    val isEmpty: Boolean get() = enabled.isEmpty()

    val all: List<DailyTask> get() = enabled.values.sortedWith(LISTED_ORDER)

    operator fun get(taskId: Int): DailyTask? = enabled[taskId]

    @Synchronized
    fun replaceWith(tasks: List<DailyTask>) {
        enabled = tasks.filter(::fitsListedId).associateBy { it.id }
    }

    @Synchronized
    fun add(task: DailyTask) {
        if (fitsListedId(task)) enabled = enabled + (task.id to task)
    }

    fun pick(
        playerId: Int,
        day: LocalDate,
    ): List<DailyTask> {
        val (bonus, regular) = all.filter { it.hasTargets }.partition { it.isBonus }
        val seed = (playerId.toLong() shl Int.SIZE_BITS) xor day.toString().hashCode().toLong()
        return regular
            .shuffled(Random(seed))
            .take(tasksPerDay().coerceAtLeast(0))
            .sortedWith(LISTED_ORDER) + bonus
    }

    private fun fitsListedId(task: DailyTask): Boolean {
        val fits = task.id in 1 until DailyTaskListing.DAY_FACTOR
        if (!fits) {
            LOGGER.warn(
                "Daily task {} skipped: its id must lie between 1 and {}",
                task.id,
                DailyTaskListing.DAY_FACTOR - 1,
            )
        }
        return fits
    }

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(DailyTaskRoster::class.java)
        val LISTED_ORDER = compareBy<DailyTask>({ it.isBonus }, { it.sortOrder }, { it.id })
    }
}
