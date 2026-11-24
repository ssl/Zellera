package zel.room.unit.path

import zel.room.Room
import zel.room.furni.FurniBehaviour
import zel.room.furni.RoomFurni

interface ClosedToWalks : FurniBehaviour {
    fun closed(
        room: Room,
        furni: RoomFurni,
    ): Boolean

    fun passage(furni: RoomFurni): Passage? = null
}
