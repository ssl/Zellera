package zel.room.life

import zel.room.RoomScope
import zel.room.unit.UnitKind

class DecoHosting(
    private val hooks: LifeHooks,
) {
    fun tick(
        scope: RoomScope,
        state: RoomLifeState,
    ) {
        if (state.ticks % LifeTiming.DECO_HOSTING_TICKS != 0L) return
        val ownerId = scope.room.info.ownerId
        val visitors =
            scope.room.units
                .all()
                .filter { (it.kind as? UnitKind.Player)?.let { player -> player.playerId != ownerId } == true }
                .map { it.index }
                .toSet()
        val stayed = visitors.count { it in state.lastVisitors }
        state.lastVisitors = visitors
        if (stayed > 0) hooks.decoHosting(ownerId, stayed)
    }
}
