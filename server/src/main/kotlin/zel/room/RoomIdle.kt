package zel.room

data class RoomIdle(
    val sleepEnabled: Boolean,
    val sleepSeconds: Int,
    val kickEnabled: Boolean,
    val kickSeconds: Int,
)
