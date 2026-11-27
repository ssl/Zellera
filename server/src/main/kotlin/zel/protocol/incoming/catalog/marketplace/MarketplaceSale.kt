package zel.protocol.incoming.catalog.marketplace

import zel.game.catalog.CatalogServices
import zel.game.catalog.marketplace.MarketplacePrices
import zel.game.catalog.marketplace.MarketplaceStanding
import zel.game.catalog.marketplace.MarketplaceTokens
import zel.game.items.OwnedFurni
import zel.net.clients.ClientConnection
import zel.protocol.generated.MakeOfferIn
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.catalog.MarketplacePostResult
import zel.protocol.outgoing.catalog.MarketplaceSellStatus

class MarketplaceSale(
    private val services: CatalogServices,
    private val tokens: MarketplaceTokens = MarketplaceTokens(services),
) {
    @Suppress("ReturnCount") // one early answer per check
    fun handle(
        client: ClientConnection,
        m: MakeOfferIn,
    ) {
        if (!services.settings.marketplaceEnabled) {
            CatalogReplies.marketplacePostResult(client, MarketplacePostResult.CLOSED)
            return
        }
        val credits = m.price
        if (m.furniType != FLOOR_TYPE && m.furniType != WALL_TYPE) return
        if (m.itemIds.isEmpty() || m.itemIds.size > MAXIMUM_ITEMS_PER_OFFER) return
        if (!m.itemIds.all { isStripId(it) }) return

        val player = checkNotNull(client.player) { "a marketplace request needs a player" }
        MarketplaceStanding.refusal(player)?.let {
            CatalogReplies.marketplaceSellStatus(client, it, 0)
            return
        }
        val items = ArrayList<OwnedFurni>()
        for (itemId in m.itemIds.mapTo(LinkedHashSet()) { Math.abs(it) }) {
            val item = player.inventory.furni[itemId]
            if (item == null || (items.isNotEmpty() && item.definition !== items.first().definition)) {
                // not in the inventory, or not one furni type: nothing is listed
                CatalogReplies.marketplacePostResult(client, MarketplacePostResult.FAILED)
                return
            }
            items.add(item)
        }

        val first = items.first()
        if (!first.definition.canTrade || !first.definition.canSell || !MarketplacePrices.isListable(credits)) {
            CatalogReplies.marketplacePostResult(client, MarketplacePostResult.FAILED)
            return
        }
        if (!tokens.mayOffer(player, items.size)) {
            CatalogReplies.marketplaceSellStatus(client, MarketplaceSellStatus.NO_TOKENS, 0)
            return
        }

        val posted = items.map { services.marketplace.list(client, it, credits) }
        tokens.spendForOffers(player, posted.count { it })
        val allPosted = posted.all { it }
        CatalogReplies.marketplacePostResult(
            client,
            if (allPosted) MarketplacePostResult.POSTED else MarketplacePostResult.FAILED,
        )
    }

    companion object {
        private const val FLOOR_TYPE = 1
        private const val WALL_TYPE = 2

        const val MAXIMUM_ITEMS_PER_OFFER = 250

        fun isStripId(id: Int): Boolean = id != 0 && id != Int.MIN_VALUE
    }
}
