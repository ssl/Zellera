package zel.room.entry

data class RoomPromotion(
    val title: String,
    val description: String,
    val minutesLeft: Int,
    val category: Int = 0,
)
