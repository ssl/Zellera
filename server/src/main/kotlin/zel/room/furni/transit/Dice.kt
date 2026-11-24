package zel.room.furni.transit

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.Placeable
import zel.room.unit.RoomUnit

class Dice(
    private val rooms: TransitRooms,
    private val rolls: DiceRolls,
) : Placeable {
    fun roll(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = rolls.roll(scope, rooms.of(scope.room), furni, unit)

    fun close(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = rolls.close(scope, rooms.of(scope.room), furni, unit)

    override fun onPickedUp(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        rooms.of(scope.room).rolls.remove(furni.id)
        furni.state = Rides.CLOSED
    }
}
