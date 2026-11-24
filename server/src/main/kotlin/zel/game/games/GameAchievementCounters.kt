package zel.game.games

import zel.game.achievements.GameEvents
import zel.game.players.Player

class GameAchievementCounters(
    private val progress: (Player, String, Int) -> Unit,
) {
    /** Every finished quest of a campaign counts once on the campaign's event; the content pack picks the campaigns. */
    fun questCompleted(
        player: Player?,
        campaignCode: String?,
    ) {
        if (player == null || campaignCode.isNullOrEmpty()) return
        progress(player, GameEvents.of(GameEvents.QUEST_CAMPAIGN_COMPLETED, campaignCode), 1)
    }

    companion object {
        val SKATEBOARD_EFFECTS: Set<Int> = setOf(71, 72)

        /** Only with a skateboard effect: a pass along a line counts one per rail; crossing one rail counts nothing. */
        fun skateRailCount(
            effectId: Int,
            inARow: Int,
        ): Pair<String, Int>? =
            when {
                effectId !in SKATEBOARD_EFFECTS || inARow < 2 -> null
                inARow == 2 -> GameEvents.FURNI_SKATEBOARD_SLIDE to 2
                else -> GameEvents.FURNI_SKATEBOARD_SLIDE to 1
            }
    }
}
