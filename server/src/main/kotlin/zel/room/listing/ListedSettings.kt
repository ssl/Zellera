package zel.room.listing

data class ListedSettings(
    val ownerId: Int,
    val ownerName: String,
    val name: String,
    val description: String,
    val doorCode: Int,
    val maxPlayers: Int,
    val category: Int,
    val score: Int,
    val tagText: String,
    val isPublicRoom: Boolean,
    val isStaffPicked: Boolean,
    val allowsOtherPets: Boolean,
)
