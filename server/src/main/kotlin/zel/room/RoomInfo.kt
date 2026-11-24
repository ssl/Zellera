package zel.room

data class RoomInfo(
    val id: Int,
    val ownerId: Int,
    val ownerName: String,
    val name: String,
    val modelName: String,
    val door: DoorMode,
    val maxUsers: Int,
    val groupId: Int,
    val categoryId: Int,
    val allowWalkThrough: Boolean,
    val visuals: RoomVisuals,
    val moderation: RoomModeration,
    val idle: RoomIdle,
    val isPublic: Boolean,
    val isStaffPicked: Boolean,
    val buildPassDoor: DoorMode? = null,
) {
    val isBuildPassLocked: Boolean get() = buildPassDoor != null
}
