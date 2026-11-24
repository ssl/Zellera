package zel.wired.trigger

import zel.room.event.RoomEvent
import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

class ClickTile(
    override val furniId: Int,
    override val settings: BoxSettings,
) : FurniTrigger {
    override val needsUser = true

    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean = event is RoomEvent.ClickTile && listens(event.furniId)
}
