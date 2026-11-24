package zel.room.furni.roller

import zel.room.RoomScope
import zel.room.unit.walk.Walking

class RollerPass(
    val scope: RoomScope,
    val walking: Walking,
    val room: RollerRoom,
) {
    val movedUnits = HashSet<Int>()
    val movedFurni = HashSet<Int>()
}
