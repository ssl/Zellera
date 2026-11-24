package zel.wired.timing

import zel.room.event.RoomEvent
import zel.wired.box.TriggerBox
import zel.wired.firing.Firing

interface OnceTrigger : TriggerBox {
    val afterMillis: Long

    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean = false
}
