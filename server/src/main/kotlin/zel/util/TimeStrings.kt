// The unit table's lengths are the definition of the units.
@file:Suppress("MagicNumber")

package zel.util

import java.time.Instant
import java.time.temporal.ChronoUnit

object TimeStrings {
    private enum class Span(
        val word: String,
        val seconds: Int,
        val stride: ChronoUnit,
    ) {
        SECOND("second", 1, ChronoUnit.SECONDS),
        MINUTE("minute", SECONDS_PER_MINUTE, ChronoUnit.MINUTES),
        HOUR("hour", SECONDS_PER_HOUR, ChronoUnit.HOURS),
        DAY("day", SECONDS_PER_DAY, ChronoUnit.DAYS),
        WEEK("week", 7 * SECONDS_PER_DAY, ChronoUnit.WEEKS),
        MONTH("month", 30 * SECONDS_PER_DAY, ChronoUnit.MONTHS),
        YEAR("year", 365 * SECONDS_PER_DAY, ChronoUnit.YEARS),
        ;

        companion object {
            fun named(word: String): Span? = entries.firstOrNull { it.word == word.lowercase().removeSuffix("s") }
        }
    }

    private class Stretch(
        val count: Long,
        val span: Span,
    )

    private val CAP = Int.MAX_VALUE.toLong()

    private val PIECES = Regex("[0-9]+|[A-Za-z]+")

    /** Capped at the largest int; a month counts 30 days, a year 365. */
    fun seconds(text: String): Int =
        stretches(text).fold(0L) { total, stretch -> minOf(total + stretch.count * stretch.span.seconds, CAP) }.toInt()

    /** In calendar units of the game's zone: a month lands on the same day of the month. */
    fun after(
        start: Instant,
        text: String,
    ): Instant {
        val from = start.atZone(GameZone.current).toLocalDateTime()
        val moved = stretches(text).fold(from) { at, stretch -> at.plus(stretch.count, stretch.span.stride) }
        return moved.atZone(GameZone.current).toInstant()
    }

    private val SHORT_MARKS = listOf(Span.DAY to "d", Span.HOUR to "h", Span.MINUTE to "m", Span.SECOND to "s")

    /** Leading zero units drop, but never from [shownFrom] down. */
    fun short(
        seconds: Int,
        smallest: ChronoUnit,
        shownFrom: ChronoUnit,
    ): String {
        var rest = seconds.coerceAtLeast(0).toLong()
        val counted =
            SHORT_MARKS.filter { (span) -> span.stride >= smallest }.map { (span, mark) ->
                val count = rest / span.seconds
                rest %= span.seconds
                Triple(count, span, mark)
            }
        return counted
            .dropWhile { (count, span) -> count == 0L && span.stride > shownFrom }
            .joinToString(" ") { (count, _, mark) -> "$count$mark" }
    }

    /** A number too big for an int or an unknown unit word drops the pair. */
    private fun stretches(text: String): List<Stretch> =
        PIECES
            .findAll(text)
            .map(MatchResult::value)
            .zipWithNext(::stretch)
            .filterNotNull()
            .toList()

    private fun stretch(
        digits: String,
        word: String,
    ): Stretch? {
        val count = digits.toIntOrNull() ?: return null
        val span = Span.named(word) ?: return null
        return Stretch(count.toLong(), span)
    }
}
