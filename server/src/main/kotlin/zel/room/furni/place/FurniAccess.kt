package zel.room.furni.place

data class FurniAccess(
    val playerId: Int,
    val isRoomOwner: Boolean,
    val hasRights: Boolean,
    val groupDecorates: Boolean,
    val staffPlaces: Boolean,
    val staffMoves: Boolean,
) {
    /** Placing floor and wall furni: never a plain rights holder. */
    val mayPlace: Boolean get() = isRoomOwner || groupDecorates || staffPlaces

    val mayMove: Boolean get() = mayPlace || hasRights || staffMoves

    val mayUseHelper: Boolean get() = isRoomOwner || hasRights

    val mayPostNotes: Boolean get() = isRoomOwner || hasRights || groupDecorates

    /** A Build Pass furni is destroyed by a pickup, so only the room's owner or its placer takes it. */
    fun pickup(
        placerId: Int,
        roomOwnerId: Int,
        isBuildPass: Boolean = false,
    ): PickupRight =
        when {
            isRoomOwner || placerId == playerId -> PickupRight.Allowed
            isBuildPass || placerId == roomOwnerId -> PickupRight.Refused
            hasRights || groupDecorates -> PickupRight.Allowed
            else -> PickupRight.Refused
        }
}
