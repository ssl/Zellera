package zel.game.catalog.marketplace

import zel.util.SECONDS_PER_HOUR

/** The client is told these terms and holds a seller to them before it sends an offer; the server holds the same. */
object MarketplacePrices {
    const val MINIMUM_LISTING = 2
    const val MAXIMUM_LISTING = 99_999
    const val COMMISSION_PERCENT = MarketplaceFee.STANDARD_ADDED_PERCENT
    const val OPEN_HOURS = 168
    const val AVERAGE_DAYS = 7

    const val OPEN_SECONDS = OPEN_HOURS * SECONDS_PER_HOUR

    fun isListable(price: Int): Boolean = price in MINIMUM_LISTING..MAXIMUM_LISTING

    fun listingPricesPaying(
        buyerAtLeast: Int,
        buyerAtMost: Int,
    ): IntRange = MarketplaceFee.Standard.listingPricesPaying(buyerAtLeast, buyerAtMost)

    /** An offer is open while `listed_at > openSince(now)`. */
    fun openSince(now: Int): Int = now - OPEN_SECONDS

    fun isOpen(
        listedAt: Int,
        now: Int,
    ): Boolean = listedAt > openSince(now)
}
