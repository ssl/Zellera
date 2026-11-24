package zel.room.furni.transit

import zel.room.RoomScope
import zel.room.furni.FurniBehaviour
import zel.room.furni.RoomFurni
import zel.room.furni.place.FurniAccess
import zel.room.unit.RoomUnit

interface TransitUse : FurniBehaviour {
    fun use(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
        access: FurniAccess,
    )
}
