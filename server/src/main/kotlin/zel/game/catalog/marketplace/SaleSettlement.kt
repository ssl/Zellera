package zel.game.catalog.marketplace

import zel.db.Database
import zel.db.allOrNothing
import zel.game.earnings.IncomeRewards
import zel.game.economy.BalanceChange
import zel.game.economy.EconomyLedger
import zel.game.economy.LedgerEntry
import zel.game.economy.LedgerTransaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection

class SaleSettlement(
    private val database: Database,
) {
    data class Sale(
        val offerId: Int,
        val furniId: Int,
        val buyerId: Int,
        val ledgerCurrency: Int,
        val charge: Int,
        val soldAt: Int,
        val sellerId: Int,
        val sellerEarnings: Int,
    ) {
        val isWellFormed: Boolean
            get() =
                offerId > 0 &&
                    furniId > 0 &&
                    buyerId > 0 &&
                    sellerId > 0 &&
                    charge > 0 &&
                    soldAt > 0 &&
                    sellerEarnings >= 0 &&
                    ledgerCurrency >= EconomyLedger.CREDITS
    }

    @Suppress("TooGenericExceptionCaught") // a refused balance and a failed statement both mean: not sold
    fun settle(sale: Sale): BalanceChange? {
        if (!sale.isWellFormed) return null
        return try {
            database.dataSource.connection.use { connection -> connection.allOrNothing { settle(connection, sale) } }
        } catch (e: Exception) {
            log.warn("Marketplace sale of offer {} to player {} not settled", sale.offerId, sale.buyerId, e)
            null
        }
    }

    private fun settle(
        connection: Connection,
        sale: Sale,
    ): BalanceChange? {
        if (!MarketplaceStore.markSold(connection, sale.offerId, sale.soldAt)) return null
        if (!MarketplaceStore.handOver(connection, sale.furniId, sale.buyerId)) return null
        val paid = LedgerTransaction(connection).post(charge(sale))
        if (sale.sellerEarnings > 0) {
            IncomeRewards.insert(
                connection,
                sale.sellerId,
                IncomeRewards.MARKETPLACE,
                IncomeRewards.CREDITS,
                sale.sellerEarnings,
                "",
                sale.soldAt,
            )
        }
        return paid
    }

    /** The key is the listing's, so the ledger takes one listing's price only once. */
    private fun charge(sale: Sale) =
        LedgerEntry(
            key = "market/${sale.offerId}/buy",
            playerId = sale.buyerId,
            currency = sale.ledgerCurrency,
            amount = -sale.charge,
            cause = "market.buy",
            actorId = sale.sellerId.takeIf { it != sale.buyerId },
            furniId = sale.furniId,
        )

    private companion object {
        val log: Logger = LoggerFactory.getLogger(SaleSettlement::class.java)
    }
}
