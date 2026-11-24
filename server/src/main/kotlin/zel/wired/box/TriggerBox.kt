package zel.wired.box

import zel.room.event.RoomEvent
import zel.wired.firing.Firing
import zel.wired.firing.FiringStart

interface TriggerBox : WiredBox {
    val needsUser: Boolean get() = false

    fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean

    /** Asked after the selectors ran; false ends the run with nothing sent. */
    fun answers(firing: Firing): Boolean = true

    fun started(start: FiringStart): FiringStart = start

    fun passed(firing: Firing) {}
}
