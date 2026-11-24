package zel.room.furni.transit

import zel.protocol.generated.ObjectDataUpdateOut
import zel.protocol.generated.ObjectDataUpdateOutCodec
import zel.protocol.generated.ObjectUpdateOutCodec
import zel.protocol.generated.ObjectsDataUpdateOut
import zel.protocol.generated.ObjectsDataUpdateOutCodec
import zel.protocol.generated.OneWayDoorStatusOut
import zel.protocol.generated.OneWayDoorStatusOutCodec
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.FurniWire
import zel.room.tick.Audience
import zel.room.tick.Lane

object StateOut {
    fun data(
        scope: RoomScope,
        furni: RoomFurni,
        state: String,
        keep: Boolean = false,
    ) {
        set(furni, state, keep)
        scope.out.send(
            Lane.Now,
            Audience.Room,
            ObjectDataUpdateOutCodec,
            ObjectDataUpdateOut(furni.id.toString(), FurniWire.stuff(furni)),
        )
    }

    /** The plural object data update, after the tick's unit update. */
    fun plural(
        scope: RoomScope,
        furni: RoomFurni,
        state: String,
    ) {
        set(furni, state, keep = false)
        scope.out.send(
            Lane.AfterUnitUpdate,
            Audience.Room,
            ObjectsDataUpdateOutCodec,
            ObjectsDataUpdateOut(listOf(ObjectsDataUpdateOut.List1Item(furni.id, FurniWire.stuff(furni)))),
        )
    }

    fun whole(
        scope: RoomScope,
        furni: RoomFurni,
        state: String,
    ) {
        set(furni, state, keep = false)
        scope.out.send(Lane.Now, Audience.Room, ObjectUpdateOutCodec, FurniWire.objectUpdate(furni))
    }

    fun oneWay(
        scope: RoomScope,
        furniId: Int,
        inside: Boolean,
    ) = scope.out.send(
        Lane.Now,
        Audience.Room,
        OneWayDoorStatusOutCodec,
        OneWayDoorStatusOut(furniId, if (inside) 1 else 0),
    )

    private fun set(
        furni: RoomFurni,
        state: String,
        keep: Boolean,
    ) {
        furni.state = state
        if (keep) furni.dirty = true
    }
}
