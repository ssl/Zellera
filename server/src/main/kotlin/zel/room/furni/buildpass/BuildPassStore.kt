package zel.room.furni.buildpass

interface BuildPassStore {
    fun account(playerId: Int): BuildPassAccount

    fun tracked(clientId: Int): BuildPassTracked?

    fun groupAdmins(groupId: Int): List<Int>

    fun create(
        placerId: Int,
        definitionId: Int,
        roomId: Int,
        state: String,
    ): BuildPassRow?

    /** Destroys the furni: a Build Pass furni never goes to an inventory. */
    fun delete(rowId: Int)

    fun ownedRooms(ownerId: Int): List<BuildPassOwnedRoom>

    fun lockRoom(roomId: Int)

    fun unlockRoom(roomId: Int)
}
