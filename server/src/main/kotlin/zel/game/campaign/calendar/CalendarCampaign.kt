package zel.game.campaign.calendar

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Random

/**
 * A calendar has a door for every day from [firstDay] to [lastDay]; doors count from 0.
 * A missed door stays open for [catchUpDays] more days; null keeps every passed door open.
 */
data class CalendarCampaign(
    val id: Int,
    val name: String,
    val image: String,
    val firstDay: LocalDate,
    val lastDay: LocalDate,
    val catchUpDays: Int?,
    val prizes: List<CalendarPrize> = emptyList(),
) {
    val days: Int get() = maxOf(0, doorOf(lastDay) + 1)

    /** The door of [date]; negative before the first day. */
    fun doorOf(date: LocalDate): Int = ChronoUnit.DAYS.between(firstDay, date).toInt()

    /** From its first day to its last; the client has no door for a day after that. */
    fun isRunning(today: LocalDate): Boolean = doorOf(today) in 0 until days

    /** [force] opens any door of a running calendar. */
    fun opens(
        day: Int,
        today: LocalDate,
        force: Boolean,
    ): Boolean =
        isRunning(today) && prizes.isNotEmpty() && day in 0 until days && (force || isOpenOn(day, doorOf(today)))

    /** The doors the client shows shut: the ones to come and the ones missed for too long. */
    fun shutDoors(
        today: LocalDate,
        opened: Collection<Int>,
    ): List<Int> {
        val door = doorOf(today)
        return (0 until days).filter { it !in opened && !isOpenOn(it, door) }
    }

    /** The prize planned for the door, else any of the prizes that are planned for no door. */
    fun prizeFor(
        day: Int,
        random: Random,
    ): CalendarPrize? {
        prizes.firstOrNull { it.day == day }?.let { return it }
        val pool = prizes.filter { it.day == null }
        return if (pool.isEmpty()) null else pool[random.nextInt(pool.size)]
    }

    private fun isOpenOn(
        day: Int,
        todaysDoor: Int,
    ): Boolean = day <= todaysDoor && (catchUpDays == null || todaysDoor - day <= catchUpDays)
}
