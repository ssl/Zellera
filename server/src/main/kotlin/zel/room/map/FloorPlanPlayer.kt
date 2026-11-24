package zel.room.map

data class FloorPlanPlayer(
    val playerId: Int,
    val mayEdit: Boolean,
    val anyRoomOwner: Boolean,
    /** The alert text a player who may not edit gets; null: the save gets no answer. */
    val noPermissionText: String?,
)
