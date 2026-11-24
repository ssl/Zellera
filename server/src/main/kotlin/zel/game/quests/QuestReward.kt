package zel.game.quests

import zel.game.economy.Currencies
import java.util.Locale

sealed interface QuestReward {
    /** Activity points of [pointType]: -1 credits, else a currency of the content. */
    data class Currency(
        val pointType: Int,
        val amount: Int,
    ) : QuestReward

    data class Badge(
        val code: String,
    ) : QuestReward

    data class Furni(
        val className: String,
        val amount: Int,
    ) : QuestReward

    data class Unknown(
        val type: String,
    ) : QuestReward

    companion object {
        fun of(
            type: String,
            extra: String,
            amount: Int,
        ): QuestReward =
            when (type.trim().lowercase(Locale.ROOT)) {
                "badge" -> Badge(extra)
                "furni" -> Furni(extra, amount)
                else -> Currencies.idOf(type)?.let { Currency(it, amount) } ?: Unknown(type)
            }
    }
}
