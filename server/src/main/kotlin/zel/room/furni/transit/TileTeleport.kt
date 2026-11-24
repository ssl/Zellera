package zel.room.furni.transit

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Steppable
import zel.room.furni.place.ExtraOnWire
import zel.room.furni.place.FurniAccess
import zel.room.unit.RoomUnit

class TileTeleport(
    private val rooms: TransitRooms,
    private val rides: Rides,
    private val hooks: TransitHooks,
    private val data: TransitData,
    private val clubOnly: Boolean,
) : Steppable,
    TransitUse,
    ExtraOnWire {
    override fun onStepOn(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        if (clubOnly && !hooks.clubMember(unit)) return
        rides.stepOnTile(scope, rooms.of(scope.room), furni, unit)
    }

    override fun onStepOff(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = rides.leftTile(scope, rooms.of(scope.room), furni, unit)

    override fun use(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
        access: FurniAccess,
    ) {
        if (clubOnly && !hooks.clubMember(unit)) return
        rides.useTile(scope, rooms.of(scope.room), furni, unit)
    }

    override fun extra(furni: RoomFurni): Int = data.shownTarget(furni.id)
}
