package zel.game.icons

import zel.content.ClientStrings
import zel.game.achievements.AchievementTally
import zel.game.players.Player

object IconAchievements {
    fun used(
        player: Player?,
        tally: AchievementTally,
    ) {
        if (player != null) ClientStrings[ClientStrings.ICON_USED_ACHIEVEMENT]?.let { tally.add(player, it, 1) }
    }

    fun claimed(
        player: Player?,
        snapshot: IconRoster?,
        claimedId: Int,
        tally: AchievementTally,
    ) {
        if (player != null && completesSet(snapshot, claimedId)) {
            ClientStrings[ClientStrings.ICON_SET_ACHIEVEMENT]?.let { tally.add(player, it, 1) }
        }
    }

    fun completesSet(
        snapshot: IconRoster?,
        claimedId: Int,
    ): Boolean = snapshot?.isSetReward(claimedId) == true
}
