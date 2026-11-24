package zel.game.treasurehunt

import zel.game.items.Items
import zel.game.players.Player
import zel.game.quests.QuestRewards
import zel.game.treasurehunt.TreasureHunt.Prize

internal class HuntPrizes(
    private val items: Items,
) {
    private val rewards = QuestRewards(items)

    fun give(
        player: Player,
        prizes: List<Prize>,
    ) = prizes.forEach { part ->
        when (part) {
            is Prize.Badge -> player.inventory.badges.award(part.code)
            is Prize.Points -> player.wallet.changePoints(part.type, part.amount)
            is Prize.Furni -> copies(player, part)
        }
    }

    private fun copies(
        player: Player,
        prize: Prize.Furni,
    ) {
        val item = items.getItem(prize.definitionId) ?: return
        repeat(prize.copies) { rewards.furni(player, item) }
    }
}
