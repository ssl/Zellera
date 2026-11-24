package zel.room

import zel.room.event.RoomEvent
import zel.room.tick.RoomClock
import zel.room.tick.RoomOutput

internal class EngineScope(
    override val room: Room,
    override val out: RoomOutput,
    private val clock: RoomClock,
    private val listeners: List<RoomListener>,
) : RoomScope {
    override fun now(): Long = clock.now()

    override fun raise(event: RoomEvent) {
        check(room.isLockedHere) { "room ${room.info.id}: event raised outside the room lock" }
        listeners.forEach { it.on(this, event) }
    }
}
