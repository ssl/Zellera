package zel.wired.timing

import zel.room.event.RoomEvent
import zel.wired.box.TriggerBox
import zel.wired.firing.Firing

/** [short] repeaters run on the 50 ms short timer. */
interface RepeatTrigger : TriggerBox {
    val periodMillis: Long
    val short: Boolean

    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean = false
}
