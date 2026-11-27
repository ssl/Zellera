package zel.game.players.subscriptions

import zel.util.SECONDS_PER_DAY
import java.time.Instant
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/** The club pay day falls on one day of every month, at the start of that day in the game's zone. */
object PaydaySchedule {
    /** Every month has these days; a later day would skip February. */
    val DAYS = 1..28

    fun latestAtOrBefore(
        now: Int,
        dayOfMonth: Int,
        zone: ZoneId,
    ): Int = latestPayday(now, dayOfMonth, zone).toEpochSecond().toInt()

    fun monthKey(
        now: Int,
        dayOfMonth: Int,
        zone: ZoneId,
    ): Int =
        YearMonth
            .from(latestPayday(now, dayOfMonth, zone))
            .atDay(1)
            .atStartOfDay(ZoneOffset.UTC)
            .toEpochSecond()
            .toInt()

    fun asMonthKey(
        stored: Int,
        dayOfMonth: Int,
        zone: ZoneId,
    ): Int {
        val utc = Instant.ofEpochSecond(stored.toLong()).atZone(ZoneOffset.UTC)
        val isKey = utc.dayOfMonth == 1 && utc.toLocalTime() == LocalTime.MIDNIGHT
        return if (isKey) stored else monthKey(stored, dayOfMonth, zone)
    }

    private fun latestPayday(
        now: Int,
        dayOfMonth: Int,
        zone: ZoneId,
    ): ZonedDateTime {
        val thisMonth = inMonthOf(now, dayOfMonth, zone)
        return if (thisMonth.toEpochSecond() > now) thisMonth.minusMonths(1) else thisMonth
    }

    fun firstAfter(
        now: Int,
        dayOfMonth: Int,
        zone: ZoneId,
    ): Int {
        val thisMonth = inMonthOf(now, dayOfMonth, zone)
        val next = if (thisMonth.toEpochSecond() > now) thisMonth else thisMonth.plusMonths(1)
        return next.toEpochSecond().toInt()
    }

    fun wholeDays(seconds: Int): Int = maxOf(0, seconds) / SECONDS_PER_DAY

    private fun inMonthOf(
        now: Int,
        dayOfMonth: Int,
        zone: ZoneId,
    ): ZonedDateTime =
        Instant
            .ofEpochSecond(now.toLong())
            .atZone(zone)
            .toLocalDate()
            .withDayOfMonth(dayOfMonth.coerceIn(DAYS))
            .atStartOfDay(zone)
}
