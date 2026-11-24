package zel.wired.signal

import zel.core.rules.rules
import zel.room.event.RoomEvent
import zel.room.furni.place.Interactions
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class SendMessage(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val messages: LinkMessages,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val hops = (firing.signal?.hops ?: -1) + 1
        if (hops >= rules.limits.wiredSignalHops) return
        val text = if (settings.ints.firstOrNull() == OWN_TEXT) settings.text else triggerText(firing)
        Targets
            .furni(firing, this)
            .filter { Interactions.of(it.definition) == LinkMessages.LINK }
            .forEach { messages.send(firing, it.id, LinkMail.Message(text, hops)) }
    }

    private fun triggerText(firing: Firing): String =
        (firing.event as? RoomEvent.Says)?.text ?: firing.signal?.text ?: NO_TRIGGER_TEXT

    private companion object {
        const val OWN_TEXT = 1
        const val NO_TRIGGER_TEXT = "null"
    }
}
