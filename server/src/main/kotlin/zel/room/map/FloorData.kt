package zel.room.map

import zel.room.RoomVisuals

interface FloorData {
    fun models(): List<RoomModel>

    fun settings(roomId: Int): FloorSettings?

    fun customFloor(roomId: Int): CustomFloor?

    fun saveFloorPlan(
        roomId: Int,
        floor: CustomFloor,
        visuals: RoomVisuals,
        fixedWallHeight: Int,
    )
}
