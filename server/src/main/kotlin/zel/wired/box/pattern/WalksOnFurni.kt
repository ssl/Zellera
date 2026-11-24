package zel.wired.box.pattern

import zel.room.event.RoomEvent
import zel.wired.box.BoxSettings
import zel.wired.firing.Firing
import zel.wired.trigger.FurniTrigger

class WalksOnFurni(
    override val furniId: Int,
    override val settings: BoxSettings,
) : FurniTrigger {
    override val needsUser = true

    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean = event is RoomEvent.StepOn && listens(event.furniId)
}
