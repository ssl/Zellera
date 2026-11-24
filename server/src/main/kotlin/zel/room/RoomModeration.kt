package zel.room

data class RoomModeration(
    val whoCanMute: Int,
    val whoCanKick: Int,
    val whoCanBan: Int,
    val floodFilter: Int,
)
