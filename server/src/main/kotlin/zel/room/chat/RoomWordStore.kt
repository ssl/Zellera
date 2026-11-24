package zel.room.chat

interface RoomWordStore {
    fun load(roomId: Int): List<String>

    fun add(
        roomId: Int,
        word: String,
    )

    fun remove(
        roomId: Int,
        word: String,
    )
}
