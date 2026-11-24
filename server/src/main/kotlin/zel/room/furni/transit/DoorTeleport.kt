package zel.room.furni.transit

import zel.protocol.generated.StuffData
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.behaviour.special.SpecialStuff
import zel.room.furni.place.ExtraOnWire
import zel.room.furni.place.FurniAccess
import zel.room.furni.place.Placeable
import zel.room.furni.place.StuffOnWire
import zel.room.unit.RoomUnit

class DoorTeleport(
    private val rooms: TransitRooms,
    private val rides: Rides,
    private val data: TransitData,
) : TransitUse,
    StuffOnWire,
    ExtraOnWire,
    Placeable {
    override fun use(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
        access: FurniAccess,
    ) = rides.useDoor(scope, rooms.of(scope.room), furni, unit)

    override fun stuff(furni: RoomFurni): StuffData = SpecialStuff.legacy(furni, furni.state.ifEmpty { Rides.CLOSED })

    override fun extra(furni: RoomFurni): Int = data.shownTarget(furni.id)

    override fun onPickedUp(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        furni.state = Rides.CLOSED
        data.forgetTargets(listOf(furni.id))
    }
}
