package zel.room.furni.behaviour.special

import zel.room.RoomScope
import zel.room.furni.FurniBehaviour
import zel.room.furni.RoomFurni

interface ObjectDataEditable : FurniBehaviour {
    fun edit(
        scope: RoomScope,
        furni: RoomFurni,
        values: Map<String, String>,
    )
}
