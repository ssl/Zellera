package zel.room.listing

data class NewRoomRow(
    val name: String,
    val description: String,
    val modelName: String,
    val maxUsers: Int,
    val categoryId: Int,
    val tradeMode: Int,
)
