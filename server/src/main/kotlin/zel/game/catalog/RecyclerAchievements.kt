package zel.game.catalog

import zel.game.achievements.AchievementTally
import zel.game.achievements.GameEvents
import zel.game.items.Item
import zel.game.players.Player

object RecyclerAchievements {
    const val RARE_LEVEL = 3

    fun recycled(
        player: Player?,
        recycled: Collection<Item?>,
        prizes: Map<Int, Set<Item>?>,
        prizeLevel: Int,
        tally: AchievementTally,
    ) {
        if (player == null || recycled.isEmpty()) return
        tally.add(player, GameEvents.RECYCLER_ITEMS, recycled.size)

        val stocked = prizes.filterValues { !it.isNullOrEmpty() }
        val prizeIds = stocked.values.flatMap { it.orEmpty() }.mapTo(HashSet()) { it.id }
        val ecoPrizes = recycled.count { it != null && it.id in prizeIds }
        if (ecoPrizes > 0) tally.add(player, GameEvents.RECYCLER_ECO_PRIZE, ecoPrizes)
        if (prizeLevel >= RARE_LEVEL) tally.add(player, GameEvents.RECYCLER_RARE_PRIZE, 1)
        if (prizeLevel > 1 && prizeLevel == (stocked.keys.maxOrNull() ?: 0)) {
            tally.add(player, GameEvents.RECYCLER_GRAND_PRIZE, 1)
        }
    }

    fun boxOpened(
        player: Player?,
        tally: AchievementTally,
    ) {
        if (player != null) tally.add(player, GameEvents.RECYCLER_BOX_OPENED, 1)
    }
}
