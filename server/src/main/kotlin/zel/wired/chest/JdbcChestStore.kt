package zel.wired.chest

import zel.db.Params
import zel.db.Sql
import zel.db.first
import zel.db.write
import zel.db.writeEach
import zel.game.earnings.WiredCreditEarnings
import zel.game.economy.EconomyLedger
import zel.game.economy.LedgerEntry
import zel.game.economy.LedgerTransaction
import zel.game.players.PlayerStanding
import zel.wired.store.inOneTransaction
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.time.Instant
import javax.sql.DataSource

class JdbcChestStore(
    private val dataSource: DataSource,
    private val now: () -> Int = { Instant.now().epochSecond.toInt() },
) : ChestStore {
    private val sql = Sql(dataSource)
    private val chests = JdbcChestRecords(dataSource)
    private val contracts = JdbcContractRecords(dataSource)

    override fun roomChests(roomId: Int): Map<Int, ChestRecord> = chests.ofRoom(roomId)

    override fun chest(furniId: Int): ChestRecord? = chests.of(furniId)

    override fun saveChest(
        furniId: Int,
        chest: ChestRecord,
    ) = dataSource.inOneTransaction { c -> chests.save(c, furniId, chest) }

    override fun roomContracts(roomId: Int): Map<Int, ContractRecord> = contracts.ofRoom(roomId)

    override fun contract(furniId: Int): ContractRecord? = contracts.of(furniId)

    override fun saveContract(
        furniId: Int,
        contract: ContractRecord,
    ) = dataSource.inOneTransaction { c -> contracts.save(c, furniId, contract) }

    override fun lastTransactionId(): Long = sql.select(LAST_ID).first { it.getLong(1) } ?: 0

    override fun <T : Any> settle(
        settlement: ChestSettlement,
        make: (Connection, Int, StoredFurni) -> T?,
    ): ChestSettlement.Written<T> =
        dataSource.inOneTransaction { c ->
            val parked =
                c.writeEach(PARK_ITEM, settlement.paid) {
                    int(it)
                    int(settlement.payerId)
                }
            if (parked.any { it != 1 }) throw SQLException("a furni paid into a chest is no longer the payer's")
            settlement.chests.forEach { (id, record) -> chests.save(c, id, record) }
            c.writeEach(LOG, settlement.rows) { row -> entry(row) }
            c.writeEach(LOG_FURNI, settlement.rows.flatMap(::furniLines)) { line -> furniLine(line) }
            val created =
                settlement.delivered.flatMap { (playerId, furni) ->
                    furni.mapNotNull { stored -> make(c, playerId, stored)?.let { playerId to it } }
                }
            ChestSettlement.Written(created, pay(c, settlement.payouts))
        }

    private fun pay(
        connection: Connection,
        payouts: List<ChestSettlement.Payout>,
    ): Map<Int, Int> {
        val ledger = LedgerTransaction(connection)
        val purses = HashMap<Int, Int>()
        payouts.forEach { payout ->
            if (payout.category == WiredCreditEarnings.PURSE) {
                purses[payout.playerId] = ledger.post(entry(payout)).after
            } else {
                WiredCreditEarnings.store(
                    connection,
                    payout.playerId,
                    payout.category,
                    payout.credits,
                    payout.createdAt,
                )
            }
        }
        return purses
    }

    private fun entry(payout: ChestSettlement.Payout) =
        LedgerEntry(payout.operationId, payout.playerId, EconomyLedger.CREDITS, payout.credits, PAYOUT_REASON)

    private fun Params.entry(row: TransactionRow) {
        long(row.id)
        int(row.roomId)
        int(row.chestId)
        boolean(row.coins)
        int(row.type)
        string(row.definition)
        intOrNullWhenNotPositive(row.userId)
        int(row.withdrawn)
        int(row.deposited)
        int(row.timestamp)
    }

    private class FurniLine(
        val entryId: Long,
        val added: Boolean,
        val line: DetailLine,
    )

    private fun furniLines(row: TransactionRow): List<FurniLine> =
        row.furniIn.map { FurniLine(row.id, true, it) } + row.furniOut.map { FurniLine(row.id, false, it) }

    private fun Params.furniLine(line: FurniLine) {
        long(line.entryId)
        boolean(line.added)
        boolean(line.line.wall)
        int(line.line.typeId)
        int(line.line.count)
    }

    override fun chestLogs(
        chestId: Int,
        size: Int,
        page: Int,
    ): Pair<List<TransactionRow>, Int> = logs("chest_id", chestId, size, page)

    override fun roomLogs(
        roomId: Int,
        size: Int,
        page: Int,
    ): Pair<List<TransactionRow>, Int> = logs("room_id", roomId, size, page)

    override fun transaction(id: Long): TransactionRow? {
        val entry = sql.select("$ENTRIES WHERE id = ?") { long(id) }.first(::transactionRow) ?: return null
        val furni =
            sql.select(ENTRY_FURNI) { long(id) }.rows { rs ->
                val line = DetailLine(rs.getBoolean("is_wall"), rs.getInt("sprite_id"), rs.getInt("amount"))
                rs.getBoolean("added") to line
            }
        return entry.copy(
            furniIn = furni.filter { it.first }.map { it.second },
            furniOut = furni.filterNot { it.first }.map { it.second },
        )
    }

    override fun addPending(notice: PendingNotice) {
        dataSource.inOneTransaction { c ->
            c.first(LOCK_OWNER, { int(notice.ownerId) }) { true }
            val waiting = c.first(COUNT_PENDING, { int(notice.ownerId) }) { it.getInt(1) } ?: 0
            if (waiting < MAX_PENDING) {
                c.write(ADD_PENDING) {
                    int(notice.ownerId)
                    int(notice.chestId)
                    int(notice.roomId)
                    int(notice.reason)
                    string(notice.chestName)
                    int(notice.actorId)
                    int(notice.createdAt)
                }
            }
        }
    }

    /** The stored row under the one trade rule; a player with no row may trade, as on the marketplace. */
    override fun mayTrade(playerId: Int): Boolean =
        sql.select(TRADE_STANDING) { int(playerId) }.first { row ->
            PlayerStanding.mayTrade(row.getBoolean("can_trade"), row.getInt("trade_locked_until"), now())
        } != false

    private fun logs(
        column: String,
        value: Int,
        size: Int,
        page: Int,
    ): Pair<List<TransactionRow>, Int> {
        val total =
            sql.select("SELECT COUNT(*) FROM wired_chest_log WHERE $column = ?") { int(value) }.first {
                it.getInt(1)
            }
        val rows =
            sql
                .select("$ENTRIES WHERE l.$column = ? ORDER BY l.logged_at DESC, l.id ASC LIMIT ? OFFSET ?") {
                    int(value)
                    int(size)
                    long((page - 1).coerceAtLeast(0).toLong() * size)
                }.rows(::transactionRow)
        return rows to (total ?: 0)
    }

    private fun transactionRow(rs: ResultSet): TransactionRow =
        TransactionRow(
            id = rs.getLong("id"),
            roomId = rs.getInt("room_id"),
            chestId = rs.getInt("chest_id"),
            coins = rs.getBoolean("holds_credits"),
            type = rs.getInt("cause"),
            definition = rs.getString("label").orEmpty(),
            userId = rs.getInt("player_id"),
            userName = rs.getString("player_name").orEmpty(),
            withdrawn = rs.getInt("taken"),
            deposited = rs.getInt("added"),
            timestamp = rs.getInt("logged_at"),
        )

    private companion object {
        const val MAX_PENDING = 5
        const val LAST_ID = "SELECT COALESCE(MAX(id), 0) FROM wired_chest_log"
        const val LOG =
            "INSERT INTO wired_chest_log (id, room_id, chest_id, holds_credits, cause, label, player_id, " +
                "taken, added, logged_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        const val LOG_FURNI =
            "INSERT INTO wired_chest_log_furni (entry_id, added, is_wall, sprite_id, amount) VALUES (?, ?, ?, ?, ?)"
        const val ENTRIES =
            "SELECT l.id, l.room_id, l.chest_id, l.holds_credits, l.cause, l.label, l.player_id, " +
                "COALESCE(p.name, '') AS player_name, l.taken, l.added, l.logged_at FROM wired_chest_log l " +
                "LEFT JOIN players p ON p.id = l.player_id"
        const val ENTRY_FURNI =
            "SELECT added, is_wall, sprite_id, amount FROM wired_chest_log_furni WHERE entry_id = ? " +
                "ORDER BY added DESC, is_wall, sprite_id"
        const val ADD_PENDING =
            "INSERT INTO wired_chest_notices (player_id, chest_id, room_id, reason, chest_name, actor_id, " +
                "amount, created_at) VALUES (?, ?, ?, ?, ?, ?, 0, ?)"
        const val LOCK_OWNER = "SELECT id FROM players WHERE id = ? FOR UPDATE"
        const val COUNT_PENDING = "SELECT COUNT(*) FROM wired_chest_notices WHERE player_id = ?"
        const val PARK_ITEM = "UPDATE furni SET player_id = NULL WHERE id = ? AND player_id = ? AND room_id IS NULL"
        const val PAYOUT_KIND = "credit_grant"
        const val PAYOUT_REASON = "wired.chest.payout"
        const val TRADE_STANDING =
            "SELECT COALESCE(can_trade, 1) AS can_trade, COALESCE(trade_locked_until, 0) AS trade_locked_until " +
                "FROM player_standing WHERE player_id = ?"
    }
}
