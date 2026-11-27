package zel.wired.chest

import java.sql.Connection

@Suppress("TooManyFunctions") // one store over chests, contracts, logs and notices
interface ChestStore {
    fun roomChests(roomId: Int): Map<Int, ChestRecord>

    fun chest(furniId: Int): ChestRecord?

    fun saveChest(
        furniId: Int,
        chest: ChestRecord,
    )

    fun roomContracts(roomId: Int): Map<Int, ContractRecord>

    fun contract(furniId: Int): ContractRecord?

    fun saveContract(
        furniId: Int,
        contract: ContractRecord,
    )

    fun lastTransactionId(): Long

    /** One transaction; throws, writing nothing, on a moved row. */
    fun <T : Any> settle(
        settlement: ChestSettlement,
        make: (Connection, Int, StoredFurni) -> T?,
    ): ChestSettlement.Written<T>

    fun chestLogs(
        chestId: Int,
        roomId: Int,
        size: Int,
        page: Int,
    ): Pair<List<TransactionRow>, Int>

    fun roomLogs(
        roomId: Int,
        size: Int,
        page: Int,
    ): Pair<List<TransactionRow>, Int>

    fun transaction(id: Long): TransactionRow?

    /** At most five wait; later ones are dropped. */
    fun addPending(notice: PendingNotice)

    /** For a player who may be offline, from the stored row: the trade pass and no trade lock running now. */
    fun mayTrade(playerId: Int): Boolean
}
