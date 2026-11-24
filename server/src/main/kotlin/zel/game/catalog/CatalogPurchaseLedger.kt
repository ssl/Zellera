package zel.game.catalog

import zel.db.Database
import zel.db.allOrNothing
import zel.game.economy.BalanceChange
import zel.game.economy.EconomyLedger
import zel.game.economy.LedgerEntry
import zel.game.economy.LedgerTransaction
import zel.game.players.Player
import zel.game.players.WalletWrites
import java.sql.Connection
import java.sql.SQLException

internal class CatalogPurchaseLedger(
    private val database: Database,
) {
    /** Balances in memory move only after the commit. */
    @Suppress("TooGenericExceptionCaught") // anything that breaks inside the transaction rolls it back
    @Throws(SQLException::class)
    fun <T> settle(
        player: Player,
        operationId: String,
        work: (Connection) -> PurchaseCharge<T>,
    ): T =
        WalletWrites.locked(player) {
            val settled =
                try {
                    database.dataSource.connection.use { transact(it, player.profile.id, operationId, work) }
                } catch (failure: Exception) {
                    throw failure as? SQLException ?: SQLException("purchase $operationId broke off", failure)
                } ?: throw SQLException("purchase $operationId rolled back")
            settled.changes.forEach { (debit, change) -> WalletWrites.publish(player, debit.currency, change.after) }
            settled.charge.value
        }

    private class Settled<T>(
        val charge: PurchaseCharge<T>,
        val changes: List<Pair<LedgerEntry, BalanceChange>>,
    )

    private fun <T> transact(
        connection: Connection,
        playerId: Int,
        operationId: String,
        work: (Connection) -> PurchaseCharge<T>,
    ): Settled<T>? =
        connection.allOrNothing {
            val charge = work(connection)
            val debits = debits(playerId, operationId, charge)
            val changes = if (debits.isEmpty()) emptyList() else LedgerTransaction(connection).postAll(debits)
            Settled(charge, debits.zip(changes))
        }

    private fun debits(
        playerId: Int,
        operationId: String,
        charge: PurchaseCharge<*>,
    ): List<LedgerEntry> =
        listOfNotNull(
            charge.credits.takeIf { it > 0 }?.let {
                debit(operationId, "credits", playerId, EconomyLedger.CREDITS, it)
            },
            charge.points.takeIf { it > 0 }?.let { debit(operationId, "points", playerId, charge.pointsType, it) },
        )

    private fun debit(
        operationId: String,
        part: String,
        playerId: Int,
        currency: Int,
        amount: Int,
    ) = LedgerEntry("$operationId/$part", playerId, currency, -amount, "shop.buy")
}
