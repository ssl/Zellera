package zel.room.furni

import zel.room.RoomScope

interface Ticking : FurniBehaviour {
    fun onTick(
        scope: RoomScope,
        furni: RoomFurni,
    )
}
