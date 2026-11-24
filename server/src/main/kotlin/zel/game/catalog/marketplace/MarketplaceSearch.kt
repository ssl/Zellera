package zel.game.catalog.marketplace

import zel.db.LikePattern
import zel.util.SECONDS_PER_DAY

internal class MarketplaceSearch(
    private val filter: ListingFilter,
    private val now: Int,
    private val fee: MarketplaceFee = MarketplaceFee.Standard,
) {
    /** A price bound of 0 or less means none; prices are what the buyer pays. */
    data class ListingFilter(
        val minPrice: Int,
        val maxPrice: Int,
        val text: String?,
        val ordering: Int,
        val mergeSerials: Boolean,
    ) {
        val hasText: Boolean get() = !text.isNullOrEmpty()
    }

    /** An unknown code puts the dearest kind first. */
    @Suppress("MagicNumber") // the wire codes
    enum class Ordering(
        val code: Int,
        val sql: String,
    ) {
        DEAREST_FIRST(1, "m.price DESC"),
        CHEAPEST_FIRST(2, "m.price ASC"),
        MOST_SOLD_FIRST(3, "units_today DESC"),
        LEAST_SOLD_FIRST(4, "units_today ASC"),
        MOST_LISTED_FIRST(5, "k.listing_count DESC"),
        LEAST_LISTED_FIRST(6, "k.listing_count ASC"),
        ;

        companion object {
            fun ofCode(code: Int): Ordering = entries.firstOrNull { it.code == code } ?: DEAREST_FIRST
        }
    }

    class Query(
        val sql: String,
        val parameters: List<Any>,
    )

    private val parameters = ArrayList<Any>()
    private val sql = StringBuilder()

    fun query(): Query {
        openListings()
        kinds()
        sales()
        page()
        return Query(sql.toString(), parameters.toList())
    }

    private fun openListings() {
        val prices = fee.listingPricesPaying(filter.minPrice, filter.maxPrice)
        line("WITH open AS (")
        line("  SELECT l.id, l.price, f.definition_id, f.limited_number, f.limited_total")
        line("  FROM marketplace_listings l")
        line("  JOIN furni f ON f.id = l.furni_id")
        line("  WHERE l.state = $OPEN AND l.listed_at > ? AND l.price BETWEEN ? AND ?")
        parameters += MarketplacePrices.openSince(now)
        parameters += prices.first
        parameters += prices.last
    }

    /** The kind's cheapest listing, the oldest of equals, ranks first and is the row the kind shows. */
    private fun kinds() {
        val kind = if (filter.mergeSerials) "definition_id" else "definition_id, limited_number, limited_total"
        line("), kinds AS (")
        line("  SELECT id, definition_id, limited_number, limited_total,")
        line("    COUNT(*) OVER (PARTITION BY $kind) AS listing_count,")
        line("    ROW_NUMBER() OVER (PARTITION BY $kind ORDER BY price, id) AS price_rank")
        line("  FROM open")
    }

    /** What the furni sold for over the days the client names next to the average, and how often today. */
    private fun sales() {
        line("), sales AS (")
        line("  SELECT f.definition_id, ROUND(AVG(s.price)) AS average_price,")
        line("    SUM(s.sold_at >= ?) AS units_today", startOfUtcDay(now))
        line("  FROM marketplace_listings s")
        line("  JOIN furni f ON f.id = s.furni_id")
        line("  WHERE s.state = $SOLD AND s.sold_at >= ?", averageSince(now))
        line("  GROUP BY f.definition_id")
        line(")")
    }

    /** Every row carries the number of kinds found, so the first page can say how many more there are. */
    private fun page() {
        line("SELECT m.id, m.furni_id, m.player_id, m.price, m.listed_at, m.sold_at, m.state,")
        line("  k.definition_id, k.limited_number, k.limited_total, k.listing_count,")
        line("  COALESCE(sales.average_price, 0) AS average_price,")
        line("  COALESCE(sales.units_today, 0) AS units_today,")
        line("  COUNT(*) OVER () AS kinds_found")
        line("FROM kinds k")
        line("JOIN marketplace_listings m ON m.id = k.id")
        line("JOIN furni_definitions d ON d.id = k.definition_id")
        line("LEFT JOIN sales ON sales.definition_id = k.definition_id")
        line("WHERE k.price_rank = 1")
        line("  AND EXISTS ($SHOP_OFFERS)")
        if (filter.hasText) {
            val needle = LikePattern.containing(filter.text)
            line("  AND (d.display_name LIKE ? OR EXISTS ($SHOP_OFFERS AND o.name LIKE ?))", needle, needle)
        }
        line("ORDER BY ${Ordering.ofCode(filter.ordering).sql}, k.definition_id, m.id")
        line("LIMIT $PAGE_ROWS")
    }

    private fun line(
        text: String,
        vararg values: Any,
    ) {
        sql.append(text).append('\n')
        parameters.addAll(values)
    }

    companion object {
        /** The client builds a list entry per row at once; past these it tells the player to narrow the search. */
        const val PAGE_ROWS = 250

        private const val OPEN = 1
        private const val SOLD = 2

        private const val SHOP_OFFERS =
            "SELECT 1 FROM catalogue_offer_items oi" +
                " JOIN catalogue_offers o ON o.id = oi.offer_id" +
                " JOIN catalogue_pages p ON p.id = o.page_id AND p.catalogue = 'normal'" +
                " WHERE oi.definition_id = k.definition_id AND oi.position = 0"

        fun startOfUtcDay(now: Int): Int = now - now % SECONDS_PER_DAY

        /** Today and the days before it that make up the average period the client is told. */
        fun averageSince(now: Int): Int = startOfUtcDay(now) - (MarketplacePrices.AVERAGE_DAYS - 1) * SECONDS_PER_DAY
    }
}
