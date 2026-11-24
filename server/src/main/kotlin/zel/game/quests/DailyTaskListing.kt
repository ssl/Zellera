package zel.game.quests

import zel.core.DailyRound
import java.time.Instant
import java.time.LocalDate

internal object DailyTaskListing {
    /** A listed task's id holds its day and the task: day (days since 1970) * DAY_FACTOR + task id. */
    const val DAY_FACTOR = 1024

    fun listedId(
        task: DailyTask,
        day: LocalDate,
    ): Int = (day.toEpochDay() * DAY_FACTOR + task.id).toInt()

    fun taskIdOf(listedId: Int): Int = listedId % DAY_FACTOR

    fun dayOf(listedId: Int): LocalDate = LocalDate.ofEpochDay((listedId / DAY_FACTOR).toLong())

    fun isListedId(id: Int): Boolean = id >= DAY_FACTOR

    fun secondsLeftToday(
        day: LocalDate,
        now: Instant,
        round: DailyRound = WHOLE_DAYS,
    ): Int = (endOf(day, round) - now.epochSecond).coerceAtLeast(0).toInt()

    /** The end of an earlier day minus now: what its still claimable task is listed with, never above zero. */
    fun secondsPastEnd(
        day: LocalDate,
        now: Instant,
        round: DailyRound = WHOLE_DAYS,
    ): Int = (endOf(day, round) - now.epochSecond).coerceIn(Int.MIN_VALUE.toLong(), 0L).toInt()

    fun card(
        task: DailyTask,
        day: LocalDate,
        progress: DailyTaskProgress?,
        secondsLeft: Int,
    ) = DailyTaskCard(
        listedId(task, day).toLong(),
        task,
        progress?.repeats ?: 0,
        progress?.status ?: DailyTaskStatus.OPEN,
        secondsLeft,
    )

    /**
     * A task day is a daily round, written as the date that many days after 1970: with rounds of 24 hours it is the
     * UTC date, with any other length only its order means something.
     */
    fun dayAt(
        now: Instant,
        round: DailyRound = WHOLE_DAYS,
    ): LocalDate = LocalDate.ofEpochDay(round.numberAt(now.epochSecond))

    private fun endOf(
        day: LocalDate,
        round: DailyRound,
    ): Long = round.endOf(day.toEpochDay())

    val WHOLE_DAYS = DailyRound { HOURS_PER_DAY }
    private const val HOURS_PER_DAY = 24
}
