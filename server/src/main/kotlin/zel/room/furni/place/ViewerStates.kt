package zel.room.furni.place

import zel.room.RoomScope
import zel.room.furni.RoomFurni

fun interface ViewerStates {
    fun resend(scope: RoomScope)

    /** Whether the room's viewers are sent [furni]: an invisible furni controller that is on keeps some back. */
    fun shows(
        scope: RoomScope,
        furni: RoomFurni,
    ): Boolean = true

    companion object {
        val None: ViewerStates = ViewerStates { }
    }
}
