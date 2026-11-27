package zel.wired.effect.user

import zel.room.Room
import zel.room.RoomListener
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.place.Interactions
import zel.room.unit.RoomUnit
import java.util.concurrent.ConcurrentHashMap

class UserEffectState(
    private val host: UserEffectHost = UserEffectHost.None,
) : RoomPart,
    RoomListener {
    private val rooms = ConcurrentHashMap<Room, RoomUserEffects>()

    fun of(room: Room): RoomUserEffects = rooms.getOrPut(room) { RoomUserEffects() }

    override fun load(room: Room) {
        rooms[room] = RoomUserEffects().also { it.copies += host.copiesOf(room.info.id) }
        host.loadRewardCounts(
            room.furni
                .all()
                .filter { Interactions.of(it.definition) == GiveReward.CODE }
                .map { it.id },
        )
    }

    override fun unload(scope: RoomScope) {
        rooms.remove(scope.room)
        host.forgetRewardCounts(
            scope.room.furni
                .all()
                .map { it.id },
        )
    }

    override fun on(
        scope: RoomScope,
        event: RoomEvent,
    ) {
        if (event is RoomEvent.Left) left(scope.room, event.unitIndex)
    }

    fun isFrozen(
        room: Room,
        unit: RoomUnit,
    ): Boolean = of(room).frozen.containsKey(unit.index)

    fun copyPicked(
        room: Room,
        furniId: Int,
    ) {
        val kept = of(room)
        if (kept.copies.remove(furniId)) kept.visuals.furniGone(furniId)
    }

    private fun left(
        room: Room,
        unitIndex: Int,
    ) {
        val state = of(room)
        state.frozen.remove(unitIndex)
        state.teleports.remove(unitIndex)
        state.teleportEffects.remove(unitIndex)
        state.kicking.remove(unitIndex)
        state.visuals.userLeft(unitIndex)
    }
}
