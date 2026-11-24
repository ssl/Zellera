package zel.protocol.incoming.catalog.marketplace

import zel.game.catalog.marketplace.MarketplacePrices

internal data class OfferSearchInput(
    val minPrice: Int,
    val maxPrice: Int,
    val query: String,
    val sort: Int,
) {
    companion object {
        const val MAX_QUERY_LENGTH = 30
        const val DEFAULT_SORT = 1
        private val SORTS = 1..6
        private const val NO_BOUND = -1

        fun of(
            minPrice: Int,
            maxPrice: Int,
            query: String?,
            sort: Int,
        ): OfferSearchInput {
            val min = bound(minPrice)
            val max = bound(maxPrice).let { if (min > 0 && it in 1 until min) min else it }
            return OfferSearchInput(
                min,
                max,
                query?.trim()?.take(MAX_QUERY_LENGTH).orEmpty(),
                sort.takeIf { it in SORTS } ?: DEFAULT_SORT,
            )
        }

        private fun bound(price: Int): Int =
            if (price == NO_BOUND) NO_BOUND else price.coerceIn(0, MarketplacePrices.MAXIMUM_LISTING)
    }
}
