package zel.wired.variable.addon

import zel.util.MILLIS_PER_DAY
import zel.util.MILLIS_PER_HOUR
import zel.util.MILLIS_PER_MINUTE
import zel.util.MILLIS_PER_SECOND
import zel.util.MILLIS_PER_WEEK
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields

@Suppress("MagicNumber") // the bits of the form's mask
enum class TimePart(
    val bit: Int,
    val partName: String,
) {
    MILLIS_OF_SECOND(1, "milliseconds_of_seconds"),
    SECOND_OF_MINUTE(2, "seconds_of_minute"),
    MINUTE_OF_HOUR(3, "minute_of_hour"),
    HOUR_OF_DAY(4, "hour_of_day"),
    DAY_OF_WEEK(5, "day_of_week"),
    DAY_OF_MONTH(6, "day_of_month"),
    DAY_OF_YEAR(7, "day_of_year"),
    WEEK_OF_YEAR(8, "week_of_year"),
    MONTH_OF_YEAR(9, "month_of_year"),
    YEAR(10, "year"),
    MILLISECOND(20, "millisecond"),
    SECOND(21, "second"),
    MINUTE(22, "minute"),
    HOUR(23, "hour"),
    DAY(24, "day"),
    WEEK(25, "week"),
    MONTH(26, "month"),
    ;

    fun read(
        millis: Long,
        zone: ZoneId,
    ): Long {
        val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)
        return when (this) {
            MILLIS_OF_SECOND -> Math.floorMod(millis, MILLIS_PER_SECOND)
            SECOND_OF_MINUTE -> time.second.toLong()
            MINUTE_OF_HOUR -> time.minute.toLong()
            HOUR_OF_DAY -> time.hour.toLong()
            DAY_OF_WEEK -> time.dayOfWeek.value.toLong()
            DAY_OF_MONTH -> time.dayOfMonth.toLong()
            DAY_OF_YEAR -> time.dayOfYear.toLong()
            WEEK_OF_YEAR -> time.get(WEEKS.weekOfYear()).toLong()
            MONTH_OF_YEAR -> time.monthValue.toLong()
            YEAR -> time.year.toLong()
            MILLISECOND -> millis
            SECOND -> millis / MILLIS_PER_SECOND
            MINUTE -> millis / MILLIS_PER_MINUTE
            HOUR -> millis / MILLIS_PER_HOUR
            DAY -> millis / MILLIS_PER_DAY
            WEEK -> millis / MILLIS_PER_WEEK
            MONTH -> ChronoUnit.MONTHS.between(ZonedDateTime.ofInstant(Instant.EPOCH, zone), time)
        }
    }

    fun write(
        millis: Long,
        part: Long,
        zone: ZoneId,
    ): Long {
        val delta = part - read(millis, zone)
        return when (this) {
            MILLIS_OF_SECOND, MILLISECOND -> millis + delta
            SECOND_OF_MINUTE, SECOND -> millis + delta * MILLIS_PER_SECOND
            MINUTE_OF_HOUR, MINUTE -> millis + delta * MILLIS_PER_MINUTE
            HOUR_OF_DAY, HOUR -> millis + delta * MILLIS_PER_HOUR
            DAY_OF_WEEK, DAY_OF_MONTH, DAY_OF_YEAR, DAY -> millis + delta * MILLIS_PER_DAY
            WEEK_OF_YEAR, WEEK -> millis + delta * MILLIS_PER_WEEK
            MONTH_OF_YEAR, MONTH -> calendar(millis, zone).plusMonths(delta).toInstant().toEpochMilli()
            YEAR -> calendar(millis, zone).plusYears(delta).toInstant().toEpochMilli()
        }
    }

    private fun calendar(
        millis: Long,
        zone: ZoneId,
    ): ZonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)

    companion object {
        private val WEEKS = WeekFields.of(DayOfWeek.SUNDAY, 1)

        fun picked(mask: Int): List<TimePart> = entries.filter { mask and (1 shl it.bit) != 0 }
    }
}
