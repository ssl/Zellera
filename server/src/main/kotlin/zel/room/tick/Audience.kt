package zel.room.tick

sealed interface Audience {
    data object Room : Audience

    data class Player(
        val playerId: Int,
    ) : Audience

    data class RoomExcept(
        val playerId: Int,
    ) : Audience
}
