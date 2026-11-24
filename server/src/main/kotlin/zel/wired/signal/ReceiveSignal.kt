package zel.wired.signal

import zel.room.event.RoomEvent
import zel.wired.box.BoxSettings
import zel.wired.box.TriggerBox
import zel.wired.firing.Firing
import zel.wired.firing.FurniSource

class ReceiveSignal(
    override val furniId: Int,
    override val settings: BoxSettings,
) : TriggerBox {
    private val source = settings.furniSources.firstOrNull() ?: FurniSource.PICKED

    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean = false

    fun listens(antennaId: Int): Boolean =
        when (source) {
            FurniSource.SELECTION, FurniSource.SIGNAL -> true
            else -> antennaId in settings.picks
        }

    override fun answers(firing: Firing): Boolean =
        source != FurniSource.SELECTION || firing.signal?.through in firing.selection.furniIds
}
