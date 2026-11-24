package zel.game.catalog

import zel.core.Configuration
import zel.core.rules.rules
import zel.game.catalog.marketplace.MarketplaceFee
import zel.game.economy.EconomyLedger
import org.slf4j.LoggerFactory

/** Read on each use so a configuration reload applies at once. */
class CatalogSettings(
    private val config: Configuration,
) {
    val marketplaceEnabled: Boolean get() = rules.marketplace

    val marketplaceTokensRequired: Boolean get() = rules.marketplaceTokensRequired

    val tokenBatchPrice: Int get() = rules.marketplaceTokenBatchPrice

    val tokenBatchSize: Int get() = rules.marketplaceTokenBatchSize

    /** A ledger code; any negative setting means credits. */
    val marketplaceCurrency: Int
        get() = config.getInt(MARKETPLACE_CURRENCY, EconomyLedger.CREDITS).takeIf { it >= 0 } ?: EconomyLedger.CREDITS

    /** Read on each use, so a change of model applies to the next sale and the next payout. */
    val marketplaceFee: MarketplaceFee
        get() =
            if (config.valueOrNull(MARKETPLACE_FEE_MODE)?.trim() == MarketplaceFee.SELLER_PAYS) {
                MarketplaceFee.SellerPays(
                    percent("marketplace.fee.seller_pays.percent", MarketplaceFee.STANDARD_FEE_PERCENT),
                    config.getInt("marketplace.fee.seller_pays.price_limit", MarketplaceFee.STANDARD_FEE_LIMIT),
                )
            } else {
                MarketplaceFee.BuyerAdds(
                    percent("marketplace.fee.buyer_adds.percent", MarketplaceFee.STANDARD_ADDED_PERCENT),
                )
            }

    private fun percent(
        key: String,
        standard: Int,
    ): Int = config.getInt(key, standard).coerceIn(0, WHOLE)

    val purchaseCooldown: Int get() = config.getInt("catalog.purchase.gap.seconds", 0)

    val sortUsingOrderNum: Boolean get() = config.getBoolean("catalog.offers.sorted_by_position", false)

    val targetOfferId: Int? get() = config.getInt("catalog.target_offer.id", 0).takeIf { it > 0 }

    val calendarDailyOfferId: Int? get() = config.getInt("calendar.daily_offer.offer_id", 0).takeIf { it > 0 }

    val bundleDiscount: BundleDiscount
        get() =
            BundleDiscount(
                maximumItems =
                    config.getInt("catalog.bundle_discount.max_items", BundleDiscount.STANDARD_MAXIMUM_ITEMS),
                batchSize = config.getInt("catalog.bundle_discount.batch_size", 0),
                freeItemsPerBatch = config.getInt("catalog.bundle_discount.free_per_batch", 0),
                minimumDiscountsForBonus = config.getInt("catalog.bundle_discount.bonus.min_batches", -1),
                additionalThresholds = thresholds.of(config),
            )

    val giftBoxTypes: List<Int> get() = boxTypes.of(config)

    val giftRibbonTypes: List<Int> get() = ribbonTypes.of(config)

    val giftWrappingPrice: Int get() = config.getInt("catalog.gifts.wrapping.price.credits", STANDARD_WRAPPING_PRICE)

    private val thresholds =
        IntListSetting("catalog.bundle_discount.bonus.thresholds", ";", BundleDiscount.STANDARD_THRESHOLDS)
    private val boxTypes = IntListSetting("catalog.gifts.box_types", ",", STANDARD_GIFT_BOX_TYPES)
    private val ribbonTypes = IntListSetting("catalog.gifts.ribbon_types", ",", STANDARD_GIFT_RIBBON_TYPES)

    fun itemOrder(): Comparator<CatalogOffer> =
        if (sortUsingOrderNum) compareBy(CatalogOffer::position) else compareBy(CatalogOffer::id)

    fun soldCreditsGoToEarnings(): Boolean = marketplaceCurrency == EconomyLedger.CREDITS

    /** Reparsed only on change; a bad value is logged once and keeps the last good list. */
    private class IntListSetting(
        private val key: String,
        private val delimiter: String,
        private val standard: List<Int>,
    ) {
        @Volatile
        private var last: Pair<String?, List<Int>> = null to standard

        fun of(config: Configuration): List<Int> {
            val value = config.valueOrNull(key)
            val known = last
            if (known.first == value) return known.second
            val parsed =
                try {
                    value?.let(::parse) ?: standard
                } catch (refused: NumberFormatException) {
                    log.error("{} is not a list of numbers, keeping the last good one: {}", key, refused.message)
                    known.second
                }
            last = value to parsed
            return parsed
        }

        private fun parse(value: String): List<Int> =
            value
                .split(delimiter)
                .let { if (it.size == 1) it else it.dropLastWhile(String::isEmpty) }
                .map(String::toInt)
    }

    companion object {
        private val log = LoggerFactory.getLogger(CatalogSettings::class.java)
        private const val STANDARD_WRAPPING_PRICE = 1
        private const val WHOLE = 100
        private const val MARKETPLACE_CURRENCY = "marketplace.price.currency"
        private const val MARKETPLACE_FEE_MODE = "marketplace.fee.mode"
        val STANDARD_GIFT_BOX_TYPES = listOf(0, 1, 2, 3, 4, 5, 6, 8)
        val STANDARD_GIFT_RIBBON_TYPES = (0..10).toList()
    }
}
