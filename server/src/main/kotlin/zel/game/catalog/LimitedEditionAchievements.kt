package zel.game.catalog

import zel.game.achievements.AchievementTally
import zel.game.achievements.GameEvents
import zel.game.players.Player

object LimitedEditionAchievements {
    @JvmStatic
    fun purchased(
        player: Player?,
        item: CatalogOffer?,
    ) {
        if (player != null && item != null) purchased(player, AchievementTally.running())
    }

    @JvmStatic
    fun purchased(
        player: Player,
        tally: AchievementTally,
    ) = tally.add(player, GameEvents.CATALOG_LIMITED_BOUGHT, 1)

    @JvmStatic
    fun wonReleaseRaffle(player: Player?) {
        if (player != null) wonReleaseRaffle(player, AchievementTally.running())
    }

    @JvmStatic
    fun wonReleaseRaffle(
        player: Player,
        tally: AchievementTally,
    ) = tally.add(player, GameEvents.CATALOG_LIMITED_EARLY, 1)
}
