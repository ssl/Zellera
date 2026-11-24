package zel.game.catalog.marketplace

import zel.protocol.generated.MarketPlaceOffersOut
import java.util.concurrent.ConcurrentHashMap

class MarketplaceOfferCache {
    private val entries = ConcurrentHashMap<Int, MarketPlaceOffersOut>()

    @Volatile private var fee: MarketplaceFee? = null

    operator fun get(key: Int): MarketPlaceOffersOut? = entries[key]

    operator fun set(
        key: Int,
        message: MarketPlaceOffersOut,
    ) {
        entries[key] = message
    }

    /** The cached lists carry buyer prices, so they are only good for the fee they were made under. */
    fun under(fee: MarketplaceFee): MarketplaceOfferCache {
        if (this.fee != fee) {
            entries.clear()
            this.fee = fee
        }
        return this
    }

    fun clear() = entries.clear()
}
