package zel.game.webcommands

import zel.game.economy.BalanceChange
import zel.game.economy.LedgerEntry
import zel.game.economy.LedgerTransaction
import java.sql.Connection
import java.sql.SQLException

interface WalletLedger {
    @Throws(SQLException::class)
    fun lockedBalance(
        connection: Connection,
        userId: Int,
        currencyType: Int,
    ): Int

    @Throws(SQLException::class)
    fun apply(
        connection: Connection,
        operation: LedgerEntry,
    ): BalanceChange

    object Standard : WalletLedger {
        override fun lockedBalance(
            connection: Connection,
            userId: Int,
            currencyType: Int,
        ): Int = LedgerTransaction(connection).lockedBalance(userId, currencyType)

        override fun apply(
            connection: Connection,
            operation: LedgerEntry,
        ): BalanceChange = LedgerTransaction(connection).post(operation)
    }
}
