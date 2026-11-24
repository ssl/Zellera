package zel.game.quests

import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.orFallback
import java.sql.Date
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.function.IntPredicate
import javax.sql.DataSource

/** A day is stored complete when its last regular task reaches its goal; it cannot be derived later. */
internal object DailyTaskStreaks {
    const val LOOK_BACK_DAYS = 40

    interface CompleteDays {
        fun add(
            userId: Int,
            day: LocalDate,
        )

        fun between(
            userId: Int,
            from: LocalDate,
            until: LocalDate,
        ): Set<LocalDate>
    }

    fun dayComplete(
        tasks: List<DailyTask>,
        done: IntPredicate,
    ): Boolean {
        val regular = tasks.filterNot { it.isBonus }
        return regular.isNotEmpty() && regular.all { done.test(it.id) }
    }

    fun streak(
        today: LocalDate,
        completeDays: Set<LocalDate>,
    ): Int {
        var streak = 1
        for (back in 1..LOOK_BACK_DAYS) {
            if (today.minusDays(back.toLong()) !in completeDays) break
            streak++
        }
        return streak
    }

    fun completed(
        days: CompleteDays,
        userId: Int,
        today: LocalDate,
    ): Int {
        val before = days.between(userId, today.minusDays(LOOK_BACK_DAYS.toLong()), today)
        days.add(userId, today)
        return streak(today, before)
    }

    class Stored(
        dataSource: DataSource,
    ) : CompleteDays {
        private val sql = Sql(dataSource)

        override fun add(
            userId: Int,
            day: LocalDate,
        ) {
            LOGGER.orFallback("Storing the complete daily task day $day of player $userId", Unit) {
                sql.write(
                    "INSERT IGNORE INTO player_daily_task_days (player_id, day) VALUES (?, ?)",
                    userId,
                    Date.valueOf(day),
                )
            }
        }

        override fun between(
            userId: Int,
            from: LocalDate,
            until: LocalDate,
        ): Set<LocalDate> =
            LOGGER.orFallback("Reading the complete daily task days of player $userId", emptySet()) {
                sql
                    .select(
                        "SELECT day FROM player_daily_task_days WHERE player_id = ? AND day >= ? AND day < ?",
                        userId,
                        Date.valueOf(from),
                        Date.valueOf(until),
                    ).rows { it.getDate("day").toLocalDate() }
                    .toSet()
            }
    }

    class InMemory : CompleteDays {
        private val days = ConcurrentHashMap<Int, MutableSet<LocalDate>>()

        override fun add(
            userId: Int,
            day: LocalDate,
        ) {
            days.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(day)
        }

        override fun between(
            userId: Int,
            from: LocalDate,
            until: LocalDate,
        ): Set<LocalDate> = days[userId].orEmpty().filterTo(HashSet()) { !it.isBefore(from) && it.isBefore(until) }
    }

    private val LOGGER = LoggerFactory.getLogger(DailyTaskStreaks::class.java)
}
