package zel.game.catalog

import zel.game.catalog.marketplace.MarketplaceTokens
import zel.protocol.incoming.catalog.CatalogGiftPurchase
import zel.protocol.incoming.catalog.CatalogPackets
import zel.protocol.incoming.catalog.marketplace.MarketplacePackets
import zel.protocol.incoming.catalog.marketplace.MarketplaceSale
import zel.protocol.incoming.catalog.recycler.RecyclerPackets
import zel.protocol.incoming.catalog.recycler.Recycling
import zel.protocol.typed.PacketPackage

class CatalogModule(
    private val services: CatalogServices,
) {
    private val tokens = MarketplaceTokens(services)

    fun packets(): List<PacketPackage> =
        listOf(
            CatalogPackets(
                services,
                CatalogGiftPurchase(services),
            ),
            MarketplacePackets(services, MarketplaceSale(services, tokens), tokens),
            RecyclerPackets(services, Recycling(services)),
        )
}
