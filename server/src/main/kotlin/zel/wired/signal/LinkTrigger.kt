package zel.wired.signal

import zel.wired.box.TriggerBox
import zel.wired.firing.Firing
import zel.wired.firing.FurniSource
import zel.wired.firing.SignalPayload

interface LinkTrigger : TriggerBox {
    fun payload(mail: LinkMail): SignalPayload?

    fun listens(linkId: Int): Boolean =
        settings.furniSources.firstOrNull() == FurniSource.SELECTION || linkId in settings.picks

    override fun answers(firing: Firing): Boolean =
        settings.furniSources.firstOrNull() != FurniSource.SELECTION ||
            firing.signal?.through in firing.selection.furniIds
}
