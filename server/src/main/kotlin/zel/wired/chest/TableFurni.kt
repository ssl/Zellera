package zel.wired.chest

import zel.room.RoomScope

interface TableFurni {
    fun offered(
        playerId: Int,
        itemId: Int,
    ): OfferedFurni?

    fun take(
        playerId: Int,
        itemId: Int,
    ): Boolean

    fun putBack(
        playerId: Int,
        itemIds: List<Int>,
    )

    fun refreshInventory(playerId: Int)

    /** Queued behind the room's earlier chest writes; a failure returns paid furni and runs [undo]. */
    fun settle(
        roomId: Int,
        settlement: ChestSettlement,
        undo: (RoomScope) -> Unit,
    )
}
