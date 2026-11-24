package zel.game.quests

import zel.game.economy.EconomyLedger
import zel.game.items.Item
import zel.game.items.Items
import zel.game.players.Player
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** [source] is a currency's ledger reason. */
class QuestRewards(
    private val items: Items,
) {
    fun grant(
        player: Player,
        reward: QuestReward,
        source: String = SOURCE,
    ) {
        when (reward) {
            is QuestReward.Currency -> currency(player, reward, source)
            is QuestReward.Badge -> badge(player, reward.code)
            is QuestReward.Furni -> repeat(maxOf(1, reward.amount)) { furni(player, reward.className) }
            is QuestReward.Unknown -> log.warn("Reward type {} is unknown; nothing paid", reward.type)
        }
    }

    fun badge(
        player: Player,
        code: String,
    ) {
        if (code.isNotBlank()) player.inventory.badges.award(code)
    }

    private fun currency(
        player: Player,
        reward: QuestReward.Currency,
        source: String,
    ) {
        if (reward.amount <= 0) return
        if (reward.pointType == EconomyLedger.CREDITS) {
            player.wallet.changeCredits(reward.amount, source)
        } else {
            player.wallet.changePoints(reward.pointType, reward.amount, source)
        }
    }

    fun furni(
        player: Player,
        definition: Item?,
    ) {
        items.newFurni(player.profile.id, definition, 0, 0, "")?.let(player.inventory.furni::deliver)
    }

    private fun furni(
        player: Player,
        className: String,
    ) {
        val definition = items.getItem(className)
        if (definition == null) {
            log.warn("Reward furni {} does not exist; nothing given", className)
            return
        }
        furni(player, definition)
    }

    private companion object {
        const val SOURCE = "quest"
        val log: Logger = LoggerFactory.getLogger(QuestRewards::class.java)
    }
}
