package zel.game.catalog

import zel.db.Params
import zel.db.allOrNothing
import zel.db.first
import zel.db.write
import zel.game.economy.EconomyLedger
import zel.game.economy.LedgerEntry
import zel.game.economy.LedgerTransaction
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import javax.sql.DataSource

/** A use and the currency it pays are one transaction on the locked voucher row. */
class VoucherStore(
    private val dataSource: () -> DataSource,
    private val clock: () -> Int,
) {
    sealed interface Claim {
        /** [balances]: the new balance per currency the voucher paid. */
        data class Taken(
            val voucher: VoucherCode,
            val balances: Map<Int, Int>,
        ) : Claim

        data object NoSuchCode : Claim

        /** Used up, by everyone or by this player. */
        data object Spent : Claim
    }

    private class Row(
        val voucher: VoucherCode,
        val usesLeft: Int?,
        val usesPerPlayer: Int?,
    )

    @Throws(SQLException::class)
    fun claim(
        playerId: Int,
        code: String,
    ): Claim =
        dataSource().connection.use { connection ->
            connection.allOrNothing { claim(connection, playerId, code) } ?: Claim.NoSuchCode
        }

    private fun claim(
        connection: Connection,
        playerId: Int,
        code: String,
    ): Claim {
        val row = connection.first(FIND, { string(code) }, ::read) ?: return Claim.NoSuchCode
        val id = row.voucher.id
        val used = connection.first(USED, uses(id, playerId)) { it.getInt(1) } ?: 0
        val spent = row.usesLeft?.let { it <= 0 } == true || row.usesPerPlayer?.let { used >= it } == true
        if (spent) return Claim.Spent
        if (row.usesLeft != null) connection.write(TAKE) { int(id) }
        connection.write(COUNT) {
            int(id)
            int(playerId)
            int(clock())
        }
        return Claim.Taken(row.voucher, pay(connection, row.voucher, playerId, used + 1))
    }

    private fun pay(
        connection: Connection,
        voucher: VoucherCode,
        playerId: Int,
        use: Int,
    ): Map<Int, Int> {
        val amounts =
            listOf(EconomyLedger.CREDITS to voucher.credits, voucher.pointsType to voucher.points)
                .filter { (_, amount) -> amount > 0 }
        val ledger = LedgerTransaction(connection)
        return amounts.associate { (currency, amount) ->
            val key = "voucher/${voucher.id}/$playerId/$use/$currency"
            currency to ledger.post(LedgerEntry(key, playerId, currency, amount, CAUSE)).after
        }
    }

    /** For a use whose furni could not be handed out. */
    fun giveBack(
        voucherId: Int,
        playerId: Int,
    ) {
        dataSource().connection.use { connection ->
            connection.allOrNothing {
                connection.write(RETURN) { int(voucherId) }
                connection.write(UNCOUNT, uses(voucherId, playerId))
            }
        }
    }

    private fun uses(
        voucherId: Int,
        playerId: Int,
    ): Params.() -> Unit =
        {
            int(voucherId)
            int(playerId)
        }

    private fun read(row: ResultSet): Row =
        Row(
            VoucherCode(
                id = row.getInt("id"),
                credits = row.getInt("credits"),
                points = row.getInt("currency_amount"),
                pointsType = row.getInt("currency"),
                offerId = row.getInt("offer_id"),
            ),
            usesLeft = row.getInt("uses_left").takeUnless { row.wasNull() },
            usesPerPlayer = row.getInt("uses_per_player").takeUnless { row.wasNull() },
        )

    private companion object {
        const val CAUSE = "voucher.redeem"
        const val FIND =
            "SELECT id, credits, currency_amount, currency, offer_id, uses_left, uses_per_player " +
                "FROM vouchers WHERE code = ? FOR UPDATE"
        const val USED = "SELECT uses FROM voucher_uses WHERE voucher_id = ? AND player_id = ?"
        const val TAKE = "UPDATE vouchers SET uses_left = uses_left - 1 WHERE id = ?"
        const val COUNT =
            "INSERT INTO voucher_uses (voucher_id, player_id, uses, last_used_at) VALUES (?, ?, 1, ?) " +
                "ON DUPLICATE KEY UPDATE uses = uses + 1, last_used_at = VALUES(last_used_at)"
        const val RETURN = "UPDATE vouchers SET uses_left = uses_left + 1 WHERE id = ? AND uses_left IS NOT NULL"
        const val UNCOUNT =
            "UPDATE voucher_uses SET uses = uses - 1 WHERE voucher_id = ? AND player_id = ? AND uses > 0"
    }
}
