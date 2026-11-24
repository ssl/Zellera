package zel.room.listing

data class ListedPromotion(
    val title: String,
    val description: String,
    val startTimestamp: Int,
    val minutesLeft: Int,
    val category: Int = 0,
)
