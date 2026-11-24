package zel.room.furni.transit

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Steppable
import zel.room.furni.place.Placeable
import zel.room.unit.RoomUnit

class OneWayGate(
    private val rooms: TransitRooms,
    private val gates: OneWayGates,
) : Steppable,
    Placeable {
    fun enter(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = gates.enter(scope, rooms.of(scope.room), furni, unit)

    override fun onStepOn(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = Unit

    override fun onStepOff(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = gates.stepOff(scope, rooms.of(scope.room), furni, unit)

    override fun onPickedUp(
        scope: RoomScope,
        furni: RoomFurni,
    ) = gates.removed(scope, rooms.of(scope.room), furni)
}
