package zel.wired.trigger

import zel.room.event.RoomEvent
import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

class TransactionTrigger(
    override val furniId: Int,
    override val settings: BoxSettings,
    val completes: Boolean,
) : FurniTrigger {
    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean = false
}
