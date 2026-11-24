package zel.wired.trigger

import zel.room.event.RoomEvent
import zel.room.furni.place.Interactions
import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

/** wf_trg_state_changed: a state change made by wired never fires it. */
class StateChanged(
    override val furniId: Int,
    override val settings: BoxSettings,
) : FurniTrigger {
    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean = event is RoomEvent.StateChanged && listens(event.furniId) && !diceLanding(event, firing)

    /** A dice landing on its face does not fire it. */
    private fun diceLanding(
        event: RoomEvent.StateChanged,
        firing: Firing,
    ): Boolean =
        event.settled &&
            firing.scope.room.furni[event.furniId]
                ?.let { Interactions.of(it.definition) == DICE } == true

    private companion object {
        const val DICE = "chance.dice"
    }
}
