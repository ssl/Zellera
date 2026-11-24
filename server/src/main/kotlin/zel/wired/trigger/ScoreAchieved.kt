package zel.wired.trigger

import zel.room.event.RoomEvent
import zel.wired.box.BoxSettings
import zel.wired.box.TriggerBox
import zel.wired.firing.Firing

class ScoreAchieved(
    override val furniId: Int,
    override val settings: BoxSettings,
) : TriggerBox {
    private val target = settings.ints.getOrNull(0) ?: 0
    private val team = settings.ints.getOrNull(1) ?: 0

    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean =
        event is RoomEvent.ScoreChanged &&
            (team == 0 || event.team == team) &&
            event.oldTotal < target &&
            event.newTotal >= target
}
