package zel.wired.signal

import zel.room.event.RoomEvent
import zel.wired.box.BoxSettings
import zel.wired.firing.Firing
import zel.wired.firing.SignalPayload

class ReceiveMessage(
    override val furniId: Int,
    override val settings: BoxSettings,
) : LinkTrigger {
    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean = false

    override fun payload(mail: LinkMail): SignalPayload? =
        (mail as? LinkMail.Message)?.let {
            SignalPayload(emptyList(), emptyList(), emptyMap(), text = it.text, hops = it.hops)
        }
}
