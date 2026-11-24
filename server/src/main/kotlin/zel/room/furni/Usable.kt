package zel.room.furni

import zel.room.RoomScope

interface Usable : FurniBehaviour {
    fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    )
}
