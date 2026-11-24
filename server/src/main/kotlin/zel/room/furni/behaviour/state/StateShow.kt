package zel.room.furni.behaviour.state

import zel.protocol.generated.ItemUpdateOutCodec
import zel.protocol.generated.ObjectDataUpdateOut
import zel.protocol.generated.ObjectDataUpdateOutCodec
import zel.protocol.generated.ObjectUpdateOutCodec
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.FurniWire
import zel.room.tick.Audience
import zel.room.tick.Lane

object StateShow {
    fun data(
        scope: RoomScope,
        furni: RoomFurni,
        state: String,
    ) {
        set(furni, state)
        if (furni.isWall) {
            scope.out.send(Lane.Now, Audience.Room, ItemUpdateOutCodec, FurniWire.itemUpdate(furni))
        } else {
            scope.out.send(
                Lane.Now,
                Audience.Room,
                ObjectDataUpdateOutCodec,
                ObjectDataUpdateOut(furni.id.toString(), FurniWire.stuff(furni)),
            )
        }
    }

    fun whole(
        scope: RoomScope,
        furni: RoomFurni,
        state: String,
    ) {
        set(furni, state)
        scope.out.send(Lane.Now, Audience.Room, ObjectUpdateOutCodec, FurniWire.objectUpdate(furni))
    }

    fun set(
        furni: RoomFurni,
        state: String,
    ) {
        furni.state = state
        furni.dirty = true
    }
}
