package zel.room.life

import zel.room.RoomInfo
import zel.room.RoomScope

class EmptyRooms(
    private val hooks: LifeHooks,
    private val unload: (Int) -> Unit,
) {
    fun occupied(state: RoomLifeState) {
        state.emptySince = null
        state.unloadRequested = false
    }

    fun tick(
        scope: RoomScope,
        state: RoomLifeState,
    ) {
        val now = scope.now()
        val since = state.emptySince ?: now.also { state.emptySince = it }
        if (state.unloadRequested || now - since < LifeTiming.EMPTY_UNLOAD_MILLIS) return
        if (!hooks.someoneWaits(scope) && mayUnload(scope.room.info)) {
            state.unloadRequested = true
            unload(scope.room.info.id)
        }
    }

    fun mayUnload(info: RoomInfo): Boolean =
        !info.isPublic &&
            !info.isStaffPicked &&
            !hooks.publicCategory(info.categoryId) &&
            !hooks.entering(info.id)
}
