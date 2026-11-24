package zel.game.achievements

import zel.content.ClientStrings
import zel.game.players.Player

/** [named]: the achievement a client profile string names. */
class ExploreActivityAchievements(
    private val achievements: AchievementProgress,
    private val named: (String) -> Achievement? = { null },
) {
    fun marketplaceItemSold(sellerId: Int) =
        achievements.progressFor(
            sellerId,
            ClientStrings[ClientStrings.MARKETPLACE_SALES_ACHIEVEMENT]?.let(named),
            1,
        )

    fun dailyTaskCompleted(player: Player) = achievements.count(player, GameEvents.DAILY_TASK_COMPLETED)

    fun dailyTaskStreak(
        player: Player?,
        days: Int,
    ) {
        if (player == null) return
        achievements.forEvent(GameEvents.DAILY_TASK_STREAK).forEach {
            achievements.raiseTo(player, it, announce = false) { days }
        }
    }

    companion object {
        const val GYM_SECONDS_PER_POINT = 60
    }
}
