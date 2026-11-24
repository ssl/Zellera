package zel.room.furni.transit

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.FurniAccess
import zel.room.unit.RoomUnit

class Hopper(
    private val rooms: TransitRooms,
    private val rides: Rides,
    private val gates: Gates,
    private val rule: GateRule?,
) : TransitUse {
    override fun use(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
        access: FurniAccess,
    ) = rides.useHopper(scope, rooms.of(scope.room), furni, unit, mayEnter(furni, unit))

    fun mayEnter(
        furni: RoomFurni,
        unit: RoomUnit,
    ): Boolean = rule == null || gates.lets(furni, unit, rule)
}
