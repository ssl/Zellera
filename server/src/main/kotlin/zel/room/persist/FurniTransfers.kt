package zel.room.persist

interface FurniTransfers {
    fun putInRoom(
        roomId: Int,
        ownerId: Int,
        place: FurniSave,
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
