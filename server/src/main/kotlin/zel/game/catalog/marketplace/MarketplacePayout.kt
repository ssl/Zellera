package zel.game.catalog.marketplace

import zel.db.allOrNothing
import zel.game.catalog.CatalogServices
import zel.game.economy.BalanceChange
import zel.game.economy.LedgerEntry
import zel.game.economy.LedgerTransaction
import zel.game.players.Player
import zel.game.players.WalletWrites
import org.slf4j.LoggerFactory
import java.sql.Connection

/** Prices go to the wallet unless the earnings center pays them. */
internal class MarketplacePayout(
    private val services: CatalogServices,
) {
    fun collect(player: Player) {
        val listings = player.inventory.listings
        val sold = listings.all.filter { it.state == ListingState.SOLD }
        if (sold.isEmpty()) return
        val currency = MarketplaceCurrency(services.settings.marketplaceCurrency)
        val paidIn = currency.ledgerType.takeUnless { services.settings.soldCreditsGoToEarnings() }
        val sellerId = player.profile.id
        val fee = services.settings.marketplaceFee
        val due = sold.sumOf { fee.sellerGets(it.price).toLong() }
        if (paidIn != null && currency.balanceOf(player).toLong() + due > Int.MAX_VALUE) {
            log.warn("Marketplace payout of player {} does not fit the wallet", sellerId)
            return
        }
        val collected =
            WalletWrites.locked(player) {
                settle(sellerId, sold, paidIn, fee)?.also { done ->
                    done.balance?.let { WalletWrites.publish(player, currency.ledgerType, it.after) }
                }
            } ?: return
        sold.filter { it.offerId in collected.offerIds }.forEach(listings::remove)
        collected.balance?.let { currency.tell(player, it) }
    }

    @Suppress("TooGenericExceptionCaught") // a failed statement and a refused post both mean: nothing collected
    private fun settle(
        sellerId: Int,
        sold: List<MarketplaceListing>,
        paidIn: Int?,
        fee: MarketplaceFee,
    ): Collected? =
        try {
            services.database.dataSource.connection.use { connection ->
                connection.allOrNothing { collect(connection, sellerId, sold, paidIn, fee) }
            }
        } catch (e: Exception) {
            log.error("Marketplace payout of player {} not collected", sellerId, e)
            null
        }

    data class Collected(
        val offerIds: Set<Int>,
        val balance: BalanceChange?,
    )

    companion object {
        private val log = LoggerFactory.getLogger(MarketplacePayout::class.java)

        /** Each listing pays under its own key, so a collect that runs twice pays once. */
        fun collect(
            connection: Connection,
            sellerId: Int,
            sold: List<MarketplaceListing>,
            paidIn: Int?,
            fee: MarketplaceFee = MarketplaceFee.Standard,
        ): Collected {
            val detached = sold.filter { MarketplaceStore.detach(connection, it.offerId, sellerId) }
            val ledger = LedgerTransaction(connection)
            val changes =
                if (paidIn == null) {
                    emptyList()
                } else {
                    detached
                        .map { it to fee.sellerGets(it.price) }
                        .filter { (_, amount) -> amount > 0 }
                        .map { (listing, amount) -> ledger.post(payment(sellerId, paidIn, listing, amount)) }
                }
            val balance =
                changes.lastOrNull()?.let { BalanceChange(it.after, changes.sumOf(BalanceChange::amount), false) }
            return Collected(detached.mapTo(HashSet()) { it.offerId }, balance)
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
