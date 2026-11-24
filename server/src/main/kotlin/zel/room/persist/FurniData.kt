package zel.room.persist

interface FurniData {
    fun roomFurni(roomId: Int): List<FurniRow>

    /** Writes the changes of furni still in room [roomId]; a row that left the room keeps its own data. */
    fun save(
        roomId: Int,
        changes: List<FurniSave>,
    )
}
