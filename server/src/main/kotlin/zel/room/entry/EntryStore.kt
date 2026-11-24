package zel.room.entry

@Suppress("TooManyFunctions") // one per stored fact
interface EntryStore {
    fun card(roomId: Int): RoomCard?

    fun hasRights(
        roomId: Int,
        playerId: Int,
    ): Boolean

    fun banEnd(
        roomId: Int,
        playerId: Int,
    ): Int?

    fun deleteBan(
        roomId: Int,
        playerId: Int,
    )

    fun group(groupId: Int): RoomGroup?

    fun memberLevel(
        groupId: Int,
        playerId: Int,
    ): Int?

    fun promotion(
        roomId: Int,
        nowSeconds: Int,
    ): RoomPromotion?

    fun hasVoted(
        roomId: Int,
        playerId: Int,
    ): Boolean

    fun visitedBefore(
        roomId: Int,
        playerId: Int,
    ): Boolean

    fun logEntry(
        roomId: Int,
        playerId: Int,
        nowSeconds: Int,
    )

    fun openVisitStart(
        roomId: Int,
        playerId: Int,
    ): Int? = null

    fun logExit(
        roomId: Int,
        playerId: Int,
        nowSeconds: Int,
    )
}
