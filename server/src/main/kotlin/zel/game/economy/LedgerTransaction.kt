package zel.game.economy

import zel.db.first
import zel.db.write
import zel.game.players.Balance
import java.sql.Connection
import java.sql.SQLException
import java.sql.SQLIntegrityConstraintViolationException

class LedgerTransaction(
    private val connection: Connection,
) {
    /**
     * Writes the entry before the balance: the key's unique index is what finds a repeat, so a first posting
     * costs no lookup, and a repeat answers with the balance it left the first time.
     */
    @Throws(SQLException::class)
    fun post(entry: LedgerEntry): BalanceChange {
        val held = lock(entry.playerId, entry.currency)
        val after = (Balance.of(held ?: 0) + entry.amount).amount
        if (!record(entry, after)) return repeatOf(entry)
        connection.write(if (held == null) OPEN_BALANCE else SET_BALANCE) {
            int(after)
            int(entry.playerId)
            int(entry.currency)
        }
        return BalanceChange(after, entry.amount, replayed = false)
    }

    @Throws(SQLException::class)
    fun postAll(batch: List<LedgerEntry>): List<BalanceChange> {
        requireOnePlayer(batch)
        return batch.map(::post)
    }

    /** The amount of a set-balance change is the target minus this balance. */
    @Throws(SQLException::class)
    fun lockedBalance(
        playerId: Int,
        currency: Int,
    ): Int {
        require(currency >= EconomyLedger.CREDITS) { "unknown currency $currency" }
        return lock(playerId, currency) ?: 0
    }

    /** Null: the player has no row for this currency yet. */
    private fun lock(
        playerId: Int,
        currency: Int,
    ): Int? {
        val held =
            connection.first(LOCK_PLAYER, {
                int(currency)
                int(playerId)
            }) { row -> Held(row.getInt("amount").takeUnless { row.wasNull() }) }
        return (held ?: throw UnknownLedgerPlayer(playerId)).amount
    }

    private class Held(
        val amount: Int?,
    )

    private fun record(
        entry: LedgerEntry,
        after: Int,
    ): Boolean =
        try {
            connection.write(RECORD) {
                string(entry.key)
                int(entry.playerId)
                int(entry.currency)
                int(entry.amount)
                int(after)
                string(entry.cause)
                intOrNull(entry.actorId ?: NONE)
                intOrNull(entry.furniId ?: NONE)
                string(entry.note.take(LedgerEntry.MAX_NOTE))
            }
            true
        } catch (_: SQLIntegrityConstraintViolationException) {
            false
        }

    /** A key stands for one change: the same key with another player, currency, amount or cause is a bug. */
    private fun repeatOf(entry: LedgerEntry): BalanceChange =
        connection.first(RECORDED, { string(entry.key) }) { row ->
            val same =
                row.getInt("player_id") == entry.playerId &&
                    row.getInt("currency") == entry.currency &&
                    row.getInt("amount") == entry.amount &&
                    row.getString("cause") == entry.cause
            if (!same) throw SQLException("Ledger key ${entry.key} already stands for another change")
            BalanceChange(row.getInt("balance_after"), entry.amount, replayed = true)
        } ?: throw SQLException("Ledger entry ${entry.key} was refused")

    internal companion object {
        private const val NONE = 0
        private const val LOCK_PLAYER =
            "SELECT b.amount FROM players p LEFT JOIN player_balances b ON b.player_id = p.id AND b.currency = ? " +
                "WHERE p.id = ? FOR UPDATE"
        private const val OPEN_BALANCE = "INSERT INTO player_balances (amount, player_id, currency) VALUES (?, ?, ?)"
        private const val SET_BALANCE = "UPDATE player_balances SET amount = ? WHERE player_id = ? AND currency = ?"
        private const val RECORD =
            "INSERT INTO economy_entries (entry_key, player_id, currency, amount, balance_after, cause, " +
                "actor_id, furni_id, note, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, UNIX_TIMESTAMP())"
        private const val RECORDED =
            "SELECT player_id, currency, amount, cause, balance_after FROM economy_entries WHERE entry_key = ?"

        /** A batch locks one player's row: two players in one batch would have no fixed lock order. */
        fun requireOnePlayer(batch: List<LedgerEntry>) {
            require(batch.isNotEmpty()) { "a ledger batch needs an entry" }
            require(batch.distinctBy { it.playerId }.size == 1) { "a ledger batch is for one player" }
        }
    }
}
