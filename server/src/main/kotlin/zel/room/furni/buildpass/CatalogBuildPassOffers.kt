package zel.room.furni.buildpass

import zel.game.catalog.Catalog
import zel.game.catalog.CatalogMode
import zel.game.catalog.CatalogOffer
import zel.game.items.ProductKind
import zel.protocol.incoming.catalog.BuildPassPlacementExtraData
import zel.room.furni.place.Interactions

class CatalogBuildPassOffers(
    private val catalog: () -> Catalog?,
) : BuildPassOffers {
    override fun find(
        pageId: Int,
        offerId: Int,
        sent: String,
        type: ProductKind,
        rank: Int,
    ): BuildPassOffer? {
        val manager = catalog() ?: return null
        val item = item(manager, pageId, offerId) ?: return null
        if (manager.page(item.pageId, CatalogMode.BUILDER)?.opensFor(rank) != true) return null
        val definition = item.products.singleOrNull()?.takeIf { item.quantity == 1 && it.type == type } ?: return null
        if (Interactions.of(definition) == Interactions.WEB_API) return null
        val state = BuildPassPlacementExtraData.of(item, definition, sent).ifEmpty { NO_STATE }
        return BuildPassOffer(definition, state, item.clubOnly)
    }

    private fun item(
        manager: Catalog,
        pageId: Int,
        offerId: Int,
    ): CatalogOffer? {
        manager.offer(offerId, CatalogMode.BUILDER)?.let { return it }
        return manager
            .page(pageId, CatalogMode.BUILDER)
            ?.offers
            ?.values
            ?.firstOrNull { it.clientOfferId == offerId }
    }

    private companion object {
        const val NO_STATE = "0"
    }
}
