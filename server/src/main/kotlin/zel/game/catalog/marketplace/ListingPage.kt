package zel.game.catalog.marketplace

/** The first kinds of furni a search found, each as its cheapest offer, and how many kinds it found in all. */
class ListingPage(
    val listings: List<MarketplaceListing>,
    val kindsFound: Int,
) {
    companion object {
        val EMPTY = ListingPage(emptyList(), 0)
    }
}
