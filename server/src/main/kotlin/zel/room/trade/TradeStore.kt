package zel.room.trade

import zel.db.allOrNothing
import zel.db.insertForId
import zel.db.writeEach
import zel.game.economy.EconomyLedger
import zel.game.economy.LedgerEntry
import zel.game.economy.LedgerTransaction
import zel.game.items.OwnedFurni
import java.sql.Connection
import javax.sql.DataSource

class TradeStore(
    private val dataSource: DataSource,
    private val logs: () -> Boolean = { true },
) {
    /** Writes the trade in one transaction; the new credit balances, or null, writing nothing, if an item moved. */
    fun settle(record: Settled): Map<Int, Int>? =
        dataSource.connection.use { connection ->
            connection.allOrNothing {
                if (move(connection, record) && redeem(connection, record)) {
                    val logId = if (logs()) log(connection, record) else 0
                    credit(connection, record, logId)
                } else {
                    null
                }
            }
        }

    /** Each statement names the giver and an inventory row, so furni that left the inventory stops the trade. */
    private fun move(
        connection: Connection,
        record: Settled,
    ): Boolean {
        val moves = record.moves.filter { (itemId, _) -> itemId !in record.redeemed }
        if (moves.isEmpty()) return true
        return connection
            .writeEach(MOVE, moves) { (itemId, ownerId) ->
                int(ownerId)
                int(itemId)
                int(record.otherThan(ownerId))
            }.all { it == 1 }
    }

    private fun redeem(
        connection: Connection,
        record: Settled,
    ): Boolean {
        val redeemed = record.moves.filter { (itemId, _) -> itemId in record.redeemed }
        if (redeemed.isEmpty()) return true
        return connection
            .writeEach(REDEEM, redeemed) { (itemId, ownerId) ->
                int(itemId)
                int(record.otherThan(ownerId))
            }.all { it == 1 }
    }

    private fun credit(
        connection: Connection,
        record: Settled,
        logId: Int,
    ): Map<Int, Int> {
        val earned =
            record.moves
                .filter { (itemId, _) -> itemId in record.redeemed }
                .groupBy({ it.second }) { record.redeemed.getValue(it.first) }
                .mapValues { (_, credits) -> credits.fold(0, Math::addExact) }
                .filterValues { it > 0 }
        val ledger = LedgerTransaction(connection)
        return earned.keys.sorted().associateWith { playerId ->
            val entry =
                LedgerEntry(
                    key = if (logId > 0) "trade/$logId/$playerId" else LedgerEntry.newKey("trade/$playerId"),
                    playerId = playerId,
                    currency = EconomyLedger.CREDITS,
                    amount = earned.getValue(playerId),
                    cause = REDEEM_CAUSE,
                    actorId = record.otherThan(playerId),
                )
            ledger.post(entry).after
        }
    }

    private fun log(
        connection: Connection,
        record: Settled,
    ): Int {
        val logId =
            connection.insertForId(LOG) {
                int(record.firstId)
                int(record.secondId)
                string(record.firstIp)
                string(record.secondIp)
                int(record.timestamp)
            } ?: 0
        connection.writeEach(LOG_ITEM, record.moves) { (itemId, receiverId) ->
            int(logId)
            int(itemId)
            int(receiverId)
            int(record.redeemed[itemId] ?: 0)
        }
        return logId
    }

    data class Settled(
        val moves: List<Pair<Int, Int>>,
        val firstId: Int,
        val secondId: Int,
        val firstIp: String,
        val secondIp: String,
        val timestamp: Int,
        val redeemed: Map<Int, Int> = emptyMap(),
    ) {
        fun otherThan(playerId: Int): Int = if (playerId == firstId) secondId else firstId

        companion object {
            fun moves(
                items: Collection<OwnedFurni>,
                newOwner: Int,
            ): List<Pair<Int, Int>> = items.map { it.id to newOwner }
        }
    }

    private companion object {
        const val MOVE = "UPDATE furni SET player_id = ? WHERE id = ? AND player_id = ? AND room_id IS NULL"
        const val REDEEM = "DELETE FROM furni WHERE id = ? AND player_id = ? AND room_id IS NULL"
        const val LOG =
            "INSERT INTO trades (first_player_id, second_player_id, first_ip, second_ip, traded_at) VALUES (?, ?, ?, ?, ?)"
        const val LOG_ITEM =
            "INSERT INTO trade_items (trade_id, furni_id, receiver_id, redeemed_credits) VALUES (?, ?, ?, ?)"
        const val REDEEM_CAUSE = "trade.redeem"
    }
}
