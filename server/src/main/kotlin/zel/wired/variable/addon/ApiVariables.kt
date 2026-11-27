package zel.wired.variable.addon

import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.wired.variable.Availability
import zel.wired.variable.RoomVariables
import zel.wired.variable.VariableTarget
import java.util.concurrent.ConcurrentHashMap

class ApiVariables(
    private val variables: () -> RoomVariables,
) : RoomPart {
    private val rooms = ConcurrentHashMap<Room, Cached>()

    private data class Cached(
        val revision: Int,
        val names: ApiNames,
    )

    override fun load(room: Room) {
        rooms[room] = cache(room)
    }

    override fun unload(scope: RoomScope) {
        rooms.remove(scope.room)
    }

    fun of(room: Room): ApiNames {
        val revision = variables().boxRevision(room)
        rooms[room]?.takeIf { it.revision == revision }?.let { return it.names }
        return cache(room).names
    }

    private fun cache(room: Room): Cached = Cached(variables().boxRevision(room), read(room)).also { rooms[room] = it }

    private fun read(room: Room): ApiNames {
        val api =
            variables().customs(room).filter {
                Availability.isStored(it.entry.availability) && NAME.matches(it.entry.name)
            }

        fun names(target: Int) = api.filter { it.entry.target == target }.associate { it.entry.name to it.boxId }
        return ApiNames(names(VariableTarget.USER), names(VariableTarget.FURNI), names(VariableTarget.GLOBAL))
    }

    private companion object {
        val NAME = Regex("[A-Za-z0-9_]{1,40}")
    }
}
