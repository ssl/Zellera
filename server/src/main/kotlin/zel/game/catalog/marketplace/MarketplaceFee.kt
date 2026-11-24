package zel.game.catalog.marketplace

import kotlin.math.ceil

/** Who carries the marketplace fee. A listing always stores the price its seller typed. */
sealed interface MarketplaceFee {
    fun buyerPays(listed: Int): Int

    fun sellerGets(listed: Int): Int

    /** The two numbers the client's sell window computes "you will receive" from. */
    val shownPercent: Int
    val shownLimit: Int

    /** The listing prices a buyer pays within the bounds for; a bound <= 0, or a top below the bottom, is none. */
    fun listingPricesPaying(
        buyerAtLeast: Int,
        buyerAtMost: Int,
    ): IntRange {
        val all = MarketplacePrices.MINIMUM_LISTING..MarketplacePrices.MAXIMUM_LISTING
        val lowest = if (buyerAtLeast > 0) firstListing(all) { buyerPays(it) >= buyerAtLeast } else all.first
        val highest =
            if (buyerAtMost > 0 && buyerAtMost >= buyerAtLeast) {
                firstListing(all) { buyerPays(it) > buyerAtMost } - 1
            } else {
                all.last
            }
        return lowest..highest
    }

    /** The typed price is the seller's; the buyer pays [percent] of it on top, rounded up. */
    data class BuyerAdds(
        val percent: Int,
    ) : MarketplaceFee {
        override val shownPercent: Int get() = 0
        override val shownLimit: Int get() = Int.MAX_VALUE

        override fun buyerPays(listed: Int): Int {
            val added = (listed.toLong() * maxOf(0, percent) + HUNDRED - 1) / HUNDRED
            return minOf(listed + added, Int.MAX_VALUE.toLong()).toInt()
        }

        override fun sellerGets(listed: Int): Int = listed
    }

    /**
     * The typed price is the buyer's; the seller gets it less a fee of [percent] plus half the price's share of
     * [limit], computed the way the client's sell window does so both show the same number.
     */
    data class SellerPays(
        val percent: Int,
        val limit: Int,
    ) : MarketplaceFee {
        override val shownPercent: Int get() = percent
        override val shownLimit: Int get() = limit

        override fun buyerPays(listed: Int): Int = listed

        override fun sellerGets(listed: Int): Int {
            val growth = if (limit > 0) HALF * listed / limit else 0.0
            val fee = ceil(Math.round(THOUSAND * (listed * (percent / HUNDRED.toDouble() + growth))) / THOUSAND)
            return (listed - fee.toLong()).coerceIn(0, listed.toLong()).toInt()
        }
    }

    companion object {
        const val BUYER_ADDS = "buyer_adds"
        const val SELLER_PAYS = "seller_pays"
        const val STANDARD_ADDED_PERCENT = 1
        const val STANDARD_FEE_PERCENT = 2
        const val STANDARD_FEE_LIMIT = 400_000

        val Standard: MarketplaceFee = BuyerAdds(STANDARD_ADDED_PERCENT)

        private const val HUNDRED = 100L
        private const val HALF = 0.5
        private const val THOUSAND = 1000.0

        /** The first price that [holds], one past the last when none does; [holds] never turns false again. */
        private fun firstListing(
            prices: IntRange,
            holds: (Int) -> Boolean,
        ): Int {
            var low = prices.first
            var high = prices.last + 1
            while (low < high) {
                val middle = (low + high) ushr 1
                if (holds(middle)) high = middle else low = middle + 1
            }
            return low
        }
    }
}
