package zel.wired.trigger

import zel.room.event.RoomEvent
import zel.wired.box.BoxSettings
import zel.wired.firing.Firing
import zel.wired.firing.FiringStart

class StuffState(
    override val furniId: Int,
    override val settings: BoxSettings,
) : FurniTrigger {
    private val toSavedState = settings.ints.firstOrNull() == TO_SAVED_STATE

    override fun started(start: FiringStart): FiringStart = FiringStart(null, start.furni)

    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean =
        event is RoomEvent.StateChanged &&
            (event.unitIndex != null || event.settled) &&
            listens(event.furniId) &&
            (!toSavedState || reachedSaved(event.furniId, firing))

    private fun reachedSaved(
        id: Int,
        firing: Firing,
    ): Boolean {
        val saved = settings.furniStates[id] ?: settings.furniStates[-id] ?: return true
        return firing.scope.room.furni[id]
            ?.state == saved
    }

    private companion object {
        const val TO_SAVED_STATE = 1
    }
}
