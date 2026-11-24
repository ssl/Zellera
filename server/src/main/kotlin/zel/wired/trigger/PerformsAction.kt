package zel.wired.trigger

import zel.room.event.PlayerAction
import zel.room.event.RoomEvent
import zel.wired.box.BoxSettings
import zel.wired.box.TriggerBox
import zel.wired.firing.Firing

class PerformsAction(
    override val furniId: Int,
    override val settings: BoxSettings,
) : TriggerBox {
    private val action = settings.ints.firstOrNull() ?: 0

    override val needsUser = true

    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean = event is RoomEvent.Action && event.action == action && filterPasses(event.param)

    /** A sign filter names the sign id; any dance filter text matches no dance. */
    private fun filterPasses(param: String): Boolean =
        when {
            settings.text.isEmpty() -> true
            action == PlayerAction.SIGN -> settings.text == param
            action == PlayerAction.DANCE -> false
            else -> true
        }
}
