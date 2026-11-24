package zel.room.entry

data class Queued(
    val playerId: Int,
    val spectator: Boolean,
    val entry: Pending,
)
