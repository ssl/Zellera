package zel.game.players.inventory

import zel.game.catalog.marketplace.ListingState
import zel.game.catalog.marketplace.Marketplace
import zel.game.catalog.marketplace.MarketplaceListing

class OwnListings(
    private val marketplace: Marketplace,
    stored: Collection<MarketplaceListing>,
) {
    private val listings: MutableSet<MarketplaceListing> = LinkedHashSet(stored)

    val all: Set<MarketplaceListing> get() = synchronized(listings) { listings.toSet() }

    fun add(listing: MarketplaceListing) {
        synchronized(listings) { listings.add(listing) }
    }

    fun remove(listing: MarketplaceListing) {
        synchronized(listings) { listings.remove(listing) }
    }

    fun find(offerId: Int): MarketplaceListing? =
        synchronized(listings) { listings.firstOrNull { it.offerId == offerId } }

    val uncollectedCredits: Int
        get() =
            if (marketplace.soldCreditsGoToEarnings()) {
                0
            } else {
                all
                    .filter {
                        it.state == ListingState.SOLD
                    }.sumOf { marketplace.fee.sellerGets(it.price) }
            }
}
