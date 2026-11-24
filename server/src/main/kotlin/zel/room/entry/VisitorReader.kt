package zel.room.entry

import zel.game.players.Player

fun interface VisitorReader {
    fun read(
        player: Player,
        room: RoomCard,
        nowSeconds: Int,
    ): Pending
}
