package zel.room

interface RoomDirectory {
    fun isLoaded(roomId: Int): Boolean

    fun loadedRoomIds(): Set<Int>

    /** The room the player is in (entered, not still loading), or null. */
    fun roomOf(playerId: Int): Int?

    fun playersIn(roomId: Int): List<Int>
}
