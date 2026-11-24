package zel.wired.box

import zel.room.event.RoomEvent
import zel.wired.firing.Firing

/** Stands in for the trigger of a tile that has effects and none: its stack only runs when a call picks it. */
class NoTrigger(
    override val furniId: Int,
) : TriggerBox {
    override val settings = BoxSettings.EMPTY

    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ) = false
}
