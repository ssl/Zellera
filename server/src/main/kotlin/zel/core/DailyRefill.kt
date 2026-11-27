package zel.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.SQLException
import java.time.Duration
import java.time.Instant

/**
 * The daily counters are filled once for each [round]: when it begins, or at the first look of a round the server
 * was down for. The moment of the last refill is stored, so a restart neither repeats nor skips one.
 */
class DailyRefill(
    private val marks: ChoreMarks,
    private val round: DailyRound = DailyRound(),
    private val now: () -> Long = { Instant.now().epochSecond },
    private val refill: () -> Boolean,
) {
    private var filledAt: Long? = null
    private var asked = false

    @Volatile
    private var failed = false

    @Synchronized
    fun runIfDue() {
        val moment = now()
        val last = if (asked) filledAt else stored(moment)
        if (last != null && moment < round.endOf(round.numberAt(last))) return
        failed = !refill()
        if (failed) return
        filledAt = moment
        try {
            marks.write(CHORE, moment)
        } catch (e: SQLException) {
            log.error("the moment of the daily refill was not saved", e)
        }
    }

    /** Until the running round ends, where the next look belongs. */
    fun untilNextRound(): Duration {
        if (failed) return RETRY
        val moment = now()
        return Duration.ofSeconds((round.endOf(round.numberAt(moment)) - moment).coerceAtLeast(1))
    }

    /** Null when no refill was ever recorded. A record that cannot be read counts as filled just now. */
    private fun stored(moment: Long): Long? {
        filledAt =
            try {
                marks.read(CHORE)
            } catch (e: SQLException) {
                log.error("the moment of the daily refill could not be read", e)
                moment
            }
        asked = true
        return filledAt
    }

    companion object {
        const val CHORE = "respects"
        private val RETRY: Duration = Duration.ofMinutes(1)
        private val log: Logger = LoggerFactory.getLogger(DailyRefill::class.java)
    }
}
