package zel.room.persist

interface FurniTransfers {
    /** Takes the row from [ownerId]'s inventory; in the room it is [keeperId]'s (a sticky note: the room owner's). */
    fun putInRoom(
        roomId: Int,
        ownerId: Int,
        place: FurniSave,
        keeperId: Int = ownerId,
    ): Boolean

    fun takeToInventory(
        roomId: Int,
        furniId: Int,
        state: String,
    ): Boolean

    fun takeAllToInventory(
        roomId: Int,
        furni: List<FurniHandBack>,
    ): List<Int>
}
