package zel.game.players

import zel.game.achievements.GameEvents
import zel.game.achievements.count
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions

object Respect {
    fun give(
        giver: Player,
        target: Player,
    ) {
        giver.stats.tally.respectsLeft--
        giver.stats.tally.respectsGiven++
        target.stats.tally.respectsReceived++
        val achievements = giver.services.game.achievements
        achievements?.count(giver, GameEvents.PLAYER_RESPECT_GIVEN)
        achievements?.count(target, GameEvents.PLAYER_RESPECT_RECEIVED)
        PlayerActions.progress(giver, CountedAction.GIVE_RESPECT, 1)
        giver.rooms.respectShown(giver.profile.id, target.profile.id, target.stats.tally.respectsReceived)
    }
}
