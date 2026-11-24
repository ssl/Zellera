package zel.room.furni.transit

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.FurniAccess
import zel.room.unit.RoomUnit

class PlainGate(
    private val gates: Gates,
) : TransitUse {
    override fun use(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
        access: FurniAccess,
    ) = gates.use(scope, furni, unit, access)
}
