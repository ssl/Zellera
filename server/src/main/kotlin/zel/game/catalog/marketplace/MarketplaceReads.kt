package zel.game.catalog.marketplace

import org.slf4j.LoggerFactory
import zel.db.orFallback
import zel.game.catalog.CatalogServices
import zel.game.players.PlayerStanding

/** A failed read logs and answers as if nothing was found. */
internal class MarketplaceReads(
    private val services: CatalogServices,
) {
    /** A listing with the kind of furni it sells, and the seller's trade standing as stored. */
    data class Listing(
        val furniId: Int,
        val definitionId: Int,
        val sellerId: Int,
        val price: Int,
        val state: ListingState,
        val listedAt: Int,
        val sellerTradePass: Boolean = true,
        val sellerTradeLockedUntil: Int = 0,
    ) {
        fun isForSale(now: Int): Boolean =
            state == ListingState.OPEN && MarketplacePrices.isOpen(listedAt, now) && MarketplacePrices.isListable(price)

        /** What the seller's row says; an online seller's own standing is newer. */
        fun sellerMayTradeAt(now: Int): Boolean = PlayerStanding.mayTrade(sellerTradePass, sellerTradeLockedUntil, now)
    }

    data class CheapestOpen(
        val offerId: Int,
        val price: Int,
    )

    private val queries get() = services.database.queries

    fun listingsOf(playerId: Int): MutableSet<MarketplaceListing> {
        val settings = services.settings
        val unstamped = settings.marketplaceCurrency.takeUnless { settings.soldCreditsGoToEarnings() }
        return log
            .orFallback("marketplace: own offers of player $playerId", emptyList()) {
                queries.select(OWN_LISTINGS_SQL) { int(playerId) }.rows {
                    MarketplaceListing.ownRow(it, ::definition, unstamped)
                }
            }.toHashSet()
    }

    fun browse(filter: MarketplaceSearch.ListingFilter): ListingPage =
        log.orFallback("marketplace: search", ListingPage.EMPTY) {
            val query = MarketplaceSearch(filter, services.now(), services.settings.marketplaceFee).query()
            var found = 0
            val listings =
                queries
                    .select(query.sql) { query.parameters.forEach { value(it) } }
                    .rows { row ->
                        found = row.getInt("kinds_found")
                        MarketplaceListing.searchRow(row, ::definition)
                    }
            ListingPage(listings, found)
        }

    fun listing(offerId: Int): Listing? =
        log.orFallback("marketplace: offer $offerId", null) {
            queries.select(LISTING_SQL) { int(offerId) }.first { row ->
                Listing(
                    furniId = row.getInt("furni_id"),
                    definitionId = row.getInt("definition_id"),
                    sellerId = row.getInt("player_id"),
                    price = row.getInt("price"),
                    state = ListingState.fromCode(row.getInt("state")),
                    listedAt = row.getInt("listed_at"),
                    sellerTradePass = row.getBoolean("seller_can_trade"),
                    sellerTradeLockedUntil = row.getInt("seller_locked_until"),
                )
            }
        }

    /** Matches by sprite: any furni sharing [definitionId]'s sprite counts. */
    fun cheapestOpen(definitionId: Int): CheapestOpen? =
        log.orFallback("marketplace: cheapest offer like definition $definitionId", null) {
            queries
                .select(CHEAPEST_OPEN_SQL) {
                    int(MarketplacePrices.openSince(services.now()))
                    int(definitionId)
                }.first { row -> CheapestOpen(row.getInt("id"), row.getInt("price")) }
        }

    private fun definition(definitionId: Int) = services.game.items?.getItem(definitionId)

    companion object {
        private val log = LoggerFactory.getLogger(MarketplaceReads::class.java)

        private const val OWN_LISTINGS_SQL =
            "SELECT l.id, l.furni_id, l.player_id, l.price, l.listed_at, l.sold_at, l.state, l.payout_currency," +
                " f.definition_id, f.limited_number, f.limited_total" +
                " FROM marketplace_listings l LEFT JOIN furni f ON f.id = l.furni_id WHERE l.player_id = ?"

        private const val LISTING_SQL =
            "SELECT l.furni_id, f.definition_id, l.player_id, l.price, l.state, l.listed_at," +
                " COALESCE(s.can_trade, 1) AS seller_can_trade," +
                " COALESCE(s.trade_locked_until, 0) AS seller_locked_until" +
                " FROM marketplace_listings l INNER JOIN furni f ON f.id = l.furni_id" +
                " LEFT JOIN player_standing s ON s.player_id = l.player_id WHERE l.id = ?"

        private const val CHEAPEST_OPEN_SQL =
            "SELECT l.id, l.price FROM marketplace_listings l" +
                " INNER JOIN furni f ON l.furni_id = f.id" +
                " INNER JOIN furni_definitions d ON f.definition_id = d.id" +
                " WHERE l.state = 1 AND l.listed_at > ?" +
                " AND d.sprite_id = (SELECT sprite_id FROM furni_definitions WHERE id = ? LIMIT 1)" +
                " ORDER BY l.price ASC, l.id ASC LIMIT 1"
    }
}
