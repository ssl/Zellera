package zel.protocol.incoming.catalog

import zel.game.catalog.CatalogOffer
import zel.game.catalog.CatalogPage

/** The named page, or for a search result the page of the [offerBySearchId] offer. */
internal fun ShopOrder.purchasePage(
    offerBySearchId: (Int) -> CatalogOffer?,
    pageById: (Int) -> CatalogPage?,
    sellable: (CatalogPage) -> Boolean,
): CatalogPage? {
    val page =
        if (namesNoPage) {
            val offer = offerBySearchId(itemId) ?: return null
            pageById(offer.pageId)
        } else {
            pageById(pageId)
        }
    return page?.takeIf(sellable)
}
