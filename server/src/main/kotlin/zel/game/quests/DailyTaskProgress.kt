package zel.game.quests

import java.time.LocalDate

data class DailyTaskProgress(
    val taskId: Int,
    val day: LocalDate,
    val repeats: Int,
    val status: DailyTaskStatus,
) {
    val isOpen: Boolean get() = status == DailyTaskStatus.OPEN

    val isClaimable: Boolean get() = status == DailyTaskStatus.COMPLETED
}
