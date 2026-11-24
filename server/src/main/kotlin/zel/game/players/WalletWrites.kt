package zel.game.players

import zel.game.economy.BalanceChange
import zel.game.economy.EconomyLedger
import zel.game.economy.LedgerEntry
import java.sql.SQLException

/** Wallet changes hold the wallet lock from ledger commit to session update, so no reload reads a stale balance. */
object WalletWrites {
    @JvmStatic
    fun <T> locked(
        player: Player,
        work: () -> T,
    ): T = synchronized(player.profile.ledgerMutationLock) { work() }

    /** [entry] has to belong to [player]. */
    @JvmStatic
    @Throws(SQLException::class)
    fun post(
        player: Player,
        entry: LedgerEntry,
    ): BalanceChange {
        require(entry.playerId == player.profile.id) { "the entry belongs to another player" }
        return locked(player) { ledgerOf(player).post(entry).also { publish(player, entry.currency, it.after) } }
    }

    /** All [entries] have to belong to [player]. */
    @JvmStatic
    @Throws(SQLException::class)
    fun postAll(
        player: Player,
        entries: List<LedgerEntry>,
    ): List<BalanceChange> {
        require(entries.isNotEmpty()) { "a wallet batch needs entries" }
        require(entries.all { it.playerId == player.profile.id }) { "the batch holds entries of another player" }
        return locked(player) {
            ledgerOf(player).postAll(entries).also { changes ->
                entries.zip(changes).forEach { (entry, change) -> publish(player, entry.currency, change.after) }
            }
        }
    }

    @JvmStatic
    fun publish(
        player: Player,
        currency: Int,
        balance: Int,
    ) {
        val profile = player.profile
        if (currency == EconomyLedger.CREDITS) {
            profile.publishCredits(balance)
        } else {
            profile.publishBalance(currency, balance)
        }
    }

    private fun ledgerOf(player: Player): EconomyLedger =
        player.services.game.economyLedger
            .required()
}
