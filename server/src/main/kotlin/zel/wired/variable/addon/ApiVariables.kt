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
    private val rooms = ConcurrentHashMap<Room, ApiNames>()

    override fun load(room: Room) {
        rooms[room] = read(room)
    }

    override fun unload(scope: RoomScope) {
        rooms.remove(scope.room)
    }

    fun of(room: Room): ApiNames = rooms.getOrPut(room) { read(room) }

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
