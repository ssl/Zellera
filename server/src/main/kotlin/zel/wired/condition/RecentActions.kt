package zel.wired.condition

import zel.room.Room
import zel.room.RoomListener
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.unit.RoomUnit
import zel.wired.selector.ActionNote
import java.util.concurrent.ConcurrentHashMap

class RecentActions :
    RoomListener,
    RoomPart {
    private val rooms = ConcurrentHashMap<Room, MutableMap<Int, MutableMap<Int, ActionNote>>>()

    override fun on(
        scope: RoomScope,
        event: RoomEvent,
    ) {
        when (event) {
            is RoomEvent.Action -> {
                rooms.getOrPut(scope.room) { HashMap() }.getOrPut(event.unitIndex) { HashMap() }[event.action] =
                    ActionNote(event.param, scope.now())
            }

            is RoomEvent.Entered -> {
                forget(scope, event.unitIndex)
            }

            is RoomEvent.Left -> {
                forget(scope, event.unitIndex)
            }

            else -> {}
        }
    }

    private fun forget(
        scope: RoomScope,
        unitIndex: Int,
    ) {
        rooms[scope.room]?.remove(unitIndex)
    }

    override fun unload(scope: RoomScope) {
        rooms.remove(scope.room)
    }

    fun notes(
        scope: RoomScope,
        unit: RoomUnit,
    ): Map<Int, ActionNote> = rooms[scope.room]?.get(unit.index).orEmpty()

    fun did(
        scope: RoomScope,
        unit: RoomUnit,
        action: Int,
    ): Boolean = notes(scope, unit)[action]?.let { scope.now() - it.at <= WINDOW_MILLIS } == true

    companion object {
        const val WINDOW_MILLIS = 5_000L
    }
}
