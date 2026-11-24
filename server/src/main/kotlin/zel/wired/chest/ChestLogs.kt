package zel.wired.chest

import zel.protocol.generated.WiredTransactionLogDetailsOut
import zel.protocol.generated.WiredTransactionLogDetailsOutCodec
import zel.protocol.generated.WiredTransactionLogListOut
import zel.protocol.generated.WiredTransactionLogListOutCodec
import zel.protocol.typed.PacketSession
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ChestLogs(
    private val store: ChestStore,
    private val persist: (Runnable) -> Unit,
) {
    fun chestList(
        session: PacketSession,
        chestId: Int,
        pageSize: Int,
        page: Int,
    ) = list(session, TYPE_CHEST, chestId, pageSize, page) { size, p -> store.chestLogs(chestId, size, p) }

    fun roomList(
        session: PacketSession,
        roomId: Int,
        pageSize: Int,
        page: Int,
    ) = list(session, TYPE_ROOM, roomId, pageSize, page) { size, p -> store.roomLogs(roomId, size, p) }

    private fun list(
        session: PacketSession,
        type: Int,
        listId: Int,
        pageSize: Int,
        page: Int,
        read: (Int, Int) -> Pair<List<TransactionRow>, Int>,
    ) {
        val size = pageSize.coerceIn(1, MAX_PAGE_SIZE)
        val shownPage = page.coerceAtLeast(1)
        persist(
            Runnable {
                val (rows, total) = read(size, shownPage)
                session.send(
                    WiredTransactionLogListOutCodec,
                    WiredTransactionLogListOut(
                        type,
                        listId.toLong(),
                        total,
                        shownPage,
                        size,
                        rows.map(::line),
                    ),
                )
            },
        )
    }

    fun details(
        session: PacketSession,
        roomId: Int,
        transactionId: Long,
        refused: () -> Unit,
    ) = persist(
        Runnable {
            val row = store.transaction(transactionId)
            if (row == null || row.roomId != roomId) {
                refused()
            } else {
                session.send(WiredTransactionLogDetailsOutCodec, details(row))
            }
        },
    )

    private fun furniOut(row: TransactionRow) = if (row.coins) 0 else row.withdrawn

    private fun furniIn(row: TransactionRow) = if (row.coins) 0 else row.deposited

    private fun coinsOut(row: TransactionRow) = if (row.coins) row.withdrawn else 0

    private fun coinsIn(row: TransactionRow) = if (row.coins) row.deposited else 0

    private fun line(row: TransactionRow): WiredTransactionLogListOut.List1Item =
        WiredTransactionLogListOut.List1Item(
            row.id,
            row.roomId,
            row.type,
            row.definition,
            row.userId,
            row.userName,
            row.timestamp.toLong(),
            ChestLogTime.readable(row.timestamp.toLong()),
            1,
            furniOut(row),
            furniIn(row),
            coinsOut(row),
            coinsIn(row),
        )

    private fun details(row: TransactionRow): WiredTransactionLogDetailsOut =
        WiredTransactionLogDetailsOut(
            row.id,
            row.roomId,
            row.type,
            row.definition,
            row.userId,
            row.userName,
            row.timestamp.toLong(),
            ChestLogTime.readable(row.timestamp.toLong()),
            1,
            furniOut(row),
            furniIn(row),
            coinsOut(row),
            coinsIn(row),
            listOf(row.chestId),
            row.furniIn.map { WiredTransactionLogDetailsOut.List2Item(it.wall, it.typeId, "", it.count) },
            row.furniOut.map { WiredTransactionLogDetailsOut.List3Item(it.wall, it.typeId, "", it.count) },
            false,
        )

    private companion object {
        const val MAX_PAGE_SIZE = 200
        const val TYPE_CHEST = 0
        const val TYPE_ROOM = 1
    }
}

object ChestLogTime {
    // always UTC: neither the game's zone nor the room's wired zone moves the time a log row reads
    private val READABLE: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").withZone(ZoneOffset.UTC)

    fun readable(epochSeconds: Long): String = READABLE.format(Instant.ofEpochSecond(epochSeconds))
}
