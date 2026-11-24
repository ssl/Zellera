package zel.room.chat

import zel.protocol.generated.DanceOut
import zel.protocol.generated.DanceOutCodec
import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import java.util.concurrent.ConcurrentHashMap

class Dances : RoomPart {
    private val byUnit = ConcurrentHashMap<RoomUnit, Dance>()

    override fun unload(scope: RoomScope) {
        byUnit.values.removeIf { it.room === scope.room }
    }

    fun dancing(unit: RoomUnit): Boolean = byUnit.containsKey(unit)

    /** The unit's dance (1..4), 0 when it does not dance. */
    fun danceOf(unit: RoomUnit): Int = byUnit[unit]?.id ?: 0

    fun dance(
        scope: RoomScope,
        unit: RoomUnit,
        id: Int,
    ) {
        if (id == 0) byUnit.remove(unit) else byUnit[unit] = Dance(scope.room, id)
        scope.out.send(Lane.Now, Audience.Room, DanceOutCodec, DanceOut(unit.index, id))
    }

    fun left(
        room: Room,
        unitIndex: Int,
    ) {
        byUnit.entries.removeIf { (unit, dance) -> unit.index == unitIndex && dance.room === room }
    }

    fun stop(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        if (dancing(unit)) dance(scope, unit, 0)
    }

    private class Dance(
        val room: Room,
        val id: Int,
    )
}
