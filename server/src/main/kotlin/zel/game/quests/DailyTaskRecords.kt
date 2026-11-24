package zel.game.quests

import java.time.LocalDate

interface DailyTaskRecords {
    fun taskList(): List<DailyTask>

    fun dayRows(
        playerId: Int,
        day: LocalDate,
    ): List<DailyTaskProgress>

    fun leftoverClaims(
        playerId: Int,
        day: LocalDate,
    ): List<DailyTaskProgress>

    fun record(
        playerId: Int,
        progress: DailyTaskProgress,
        now: Int,
    )

    object None : DailyTaskRecords {
        override fun taskList(): List<DailyTask> = emptyList()

        override fun dayRows(
            playerId: Int,
            day: LocalDate,
        ): List<DailyTaskProgress> = emptyList()

        override fun leftoverClaims(
            playerId: Int,
            day: LocalDate,
        ): List<DailyTaskProgress> = emptyList()

        override fun record(
            playerId: Int,
            progress: DailyTaskProgress,
            now: Int,
        ) = Unit
    }
}
