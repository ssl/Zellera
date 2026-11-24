package zel.room.port

data class RoomSummary(
    val id: Int,
    val ownerId: Int,
    val ownerName: String,
    val name: String,
    val groupId: Int,
    val isPublic: Boolean = false,
)
