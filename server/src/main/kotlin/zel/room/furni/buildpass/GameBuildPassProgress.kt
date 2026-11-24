package zel.room.furni.buildpass

import zel.game.achievements.AchievementProgress
import zel.game.achievements.GameEvents
import zel.game.achievements.count
import zel.game.players.Player
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions

class GameBuildPassProgress(
    private val players: (Int) -> Player?,
    private val achievements: () -> AchievementProgress,
    private val later: (playerId: Int, job: Runnable) -> Unit,
) {
    fun placed(playerId: Int) {
        val player = players(playerId) ?: return
        PlayerActions.progress(player, CountedAction.PLACE_BUILD_PASS_FURNI, 1)
        later(playerId) { achievements().count(player, GameEvents.ROOM_BUILD_PASS_FURNI) }
    }
}
