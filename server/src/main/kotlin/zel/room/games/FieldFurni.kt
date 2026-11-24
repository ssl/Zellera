package zel.room.games

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.Placeable

open class FieldFurni : Placeable {
    override fun onPickedUp(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        furni.state = RESET_STATE
        furni.dirty = true
    }

    companion object {
        const val RESET_STATE = "0"
    }
}
