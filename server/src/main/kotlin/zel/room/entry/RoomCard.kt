package zel.room.entry

import zel.room.DoorMode

data class RoomCard(
    val id: Int,
    val ownerId: Int,
    val ownerName: String,
    val name: String,
    val description: String,
    val modelName: String,
    val door: DoorMode,
    val maxUsers: Int,
    val tradeMode: Int,
    val score: Int,
    val categoryId: Int,
    val tags: List<String>,
    val groupId: Int,
    val allowPets: Boolean,
    val staffPicked: Boolean,
    val whoCanMute: Int,
    val whoCanKick: Int,
    val whoCanBan: Int,
    val chatProtection: Int,
    val wallpaper: String,
    val floor: String,
    val landscape: String,
    val buildPassLocked: Boolean = false,
)
