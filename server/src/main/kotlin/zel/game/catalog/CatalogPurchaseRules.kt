package zel.game.catalog

import zel.core.rules.rules
import zel.game.items.CreditFurni
import zel.game.items.Item
import zel.game.items.ItemInteraction
import zel.game.players.Player

object CatalogPurchaseRules {
    /**
     * Credit furni, bots and pets sell one per order: bonus copies would hand out credits or companions for free,
     * and every pet of one order would carry the same name.
     */
    fun hasBulkDiscount(offer: CatalogOffer): Boolean =
        offer.hasOffer && offer.quantity == 1 && offer.bundle.isEmpty() && !offer.isLimited &&
            offer.products.none { CreditFurni.credits(it) > 0 || isCompanion(it) }

    private fun isCompanion(item: Item): Boolean = item.isBot || item.isPet || item.name.startsWith(RENTABLE_BOT)

    /** Null when the offer cannot be ordered in that amount. */
    fun orderedUnits(
        offer: CatalogOffer,
        amount: Int,
    ): Int? =
        when {
            amount <= 0 -> null
            offer.isLimited -> 1
            !hasBulkDiscount(offer) -> if (amount == 1) 1 else null
            amount > rules.maxItemsPerOrder -> null
            else -> amount
        }

    fun badgeDisplayBadgeOwned(
        offer: CatalogOffer,
        player: Player,
        extraData: String,
    ): Boolean =
        offer.products.none { it.interaction.name == ItemInteraction.BADGE_DISPLAY } ||
            (extraData.isNotEmpty() && player.inventory.badges.owns(extraData))

    fun isWrittenByBuyer(item: Item): Boolean =
        item.interaction.isAny(ItemInteraction.TROPHY, ItemInteraction.BADGE_DISPLAY)

    private const val RENTABLE_BOT = "rentable_bot_"
}
