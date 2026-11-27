package zel.game.catalog.marketplace

import org.slf4j.LoggerFactory
import zel.db.allOrNothing
import zel.game.catalog.CatalogServices
import zel.game.economy.BalanceChange
import zel.game.economy.LedgerEntry
import zel.game.economy.LedgerTransaction
import zel.game.players.Player
import zel.game.players.WalletWrites
import java.sql.Connection

/** Prices go to the wallet unless the earnings center pays them. */
internal class MarketplacePayout(
    private val services: CatalogServices,
) {
    fun collect(player: Player) {
        val listings = player.inventory.listings
        val sold = listings.all.filter { it.state == ListingState.SOLD }
        if (sold.isEmpty()) return
        val sellerId = player.profile.id
        val fee = services.settings.marketplaceFee
        val dueByCurrency =
            sold
                .mapNotNull { listing -> listing.payoutCurrency?.let { it to fee.sellerGets(listing.price).toLong() } }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, amounts) -> amounts.sum() }
        val overflows =
            dueByCurrency.any { (ledger, due) ->
                MarketplaceCurrency(ledger).balanceOf(player).toLong() + due > Int.MAX_VALUE
            }
        if (overflows) {
            log.warn("Marketplace payout of player {} does not fit the wallet", sellerId)
            return
        }
        val collected =
            WalletWrites.locked(player) {
                settle(sellerId, sold, fee)?.also { done ->
                    done.balances.forEach { (ledger, change) -> WalletWrites.publish(player, ledger, change.after) }
                }
            } ?: return
        sold.filter { it.offerId in collected.offerIds }.forEach(listings::remove)
        collected.balances.forEach { (ledger, change) -> MarketplaceCurrency(ledger).tell(player, change) }
    }

    @Suppress("TooGenericExceptionCaught") // a failed statement and a refused post both mean: nothing collected
    private fun settle(
        sellerId: Int,
        sold: List<MarketplaceListing>,
        fee: MarketplaceFee,
    ): Collected? =
        try {
            services.database.dataSource.connection.use { connection ->
                connection.allOrNothing { collect(connection, sellerId, sold, fee) }
            }
        } catch (e: Exception) {
            log.error("Marketplace payout of player {} not collected", sellerId, e)
            null
        }

    data class Collected(
        val offerIds: Set<Int>,
        val balances: Map<Int, BalanceChange>,
    )

    companion object {
        private val log = LoggerFactory.getLogger(MarketplacePayout::class.java)

        /** Each listing pays under its own key, so a collect that runs twice pays once. */
        fun collect(
            connection: Connection,
            sellerId: Int,
            sold: List<MarketplaceListing>,
            fee: MarketplaceFee = MarketplaceFee.Standard,
        ): Collected {
            val detached = sold.filter { MarketplaceStore.detach(connection, it.offerId, sellerId) }
            val ledger = LedgerTransaction(connection)
            val changes =
                detached
                    .mapNotNull { listing ->
                        listing.payoutCurrency?.let { Triple(listing, it, fee.sellerGets(listing.price)) }
                    }.filter { (_, _, amount) -> amount > 0 }
                    .map { (listing, paidIn, amount) ->
                        paidIn to ledger.post(payment(sellerId, paidIn, listing, amount))
                    }
            val balances =
                changes
                    .groupBy({ it.first }, { it.second })
                    .mapValues { (_, posted) ->
                        BalanceChange(posted.last().after, posted.sumOf(BalanceChange::amount), false)
                    }
            return Collected(detached.mapTo(HashSet()) { it.offerId }, balances)
        }

        private fun payment(
            sellerId: Int,
            currency: Int,
            listing: MarketplaceListing,
            amount: Int,
        ) = LedgerEntry(
            key = "market/${listing.offerId}/payout",
            playerId = sellerId,
            currency = currency,
            amount = amount,
            cause = "market.payout",
            furniId = listing.furniId.takeIf { it > 0 },
        )
    }
}
