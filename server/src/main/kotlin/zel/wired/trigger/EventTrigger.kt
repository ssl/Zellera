package zel.wired.trigger

import zel.room.event.RoomEvent
import zel.wired.box.BoxSettings
import zel.wired.box.TriggerBox
import zel.wired.firing.Firing

class EventTrigger(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val answersEvent: (RoomEvent) -> Boolean,
) : TriggerBox {
    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean = answersEvent(event)
}
