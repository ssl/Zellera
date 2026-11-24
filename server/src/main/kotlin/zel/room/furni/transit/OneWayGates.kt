package zel.room.furni.transit

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.path.TileState
import zel.room.unit.path.WalkTiles
import zel.room.unit.walk.Walking

/** Under the room lock. */
class OneWayGates(
    private val walking: Walking,
    private val holds: TransitHolds,
) {
    fun enter(
        scope: RoomScope,
        state: RoomTransit,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        val gate = furni.anchor ?: return
        if (unit.kind !is UnitKind.Player || holds.holds(unit) || furni.id in state.passages) return
        val exit = gate.step(furni.direction + HALF_TURN)
        if (unit.tile != gate.step(furni.direction) || standsOn(scope, gate) || !free(scope, exit)) return
        StateOut.oneWay(scope, furni.id, inside = true)
        state.passages[furni.id] = Passage(unit, exit)
        holds.hold(unit)
        walking.letThrough(scope, unit, setOf(furni.id))
        unit.path = listOf(gate, exit)
    }

    fun stepOff(
        scope: RoomScope,
        state: RoomTransit,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        if (state.passages[furni.id]?.unit === unit) finish(scope, state, furni.id)
    }

    fun removed(
        scope: RoomScope,
        state: RoomTransit,
        furni: RoomFurni,
    ) {
        if (furni.id in state.passages) finish(scope, state, furni.id)
    }

    fun tick(
        scope: RoomScope,
        state: RoomTransit,
    ) {
        state.passages.entries.toList().forEach { (id, passage) ->
            val unit = passage.unit
            val gate = scope.room.furni[id]?.anchor
            when {
                gate == null || scope.room.units[unit.index] !== unit -> finish(scope, state, id)
                unit.tile != gate -> if (!unit.isWalking) finish(scope, state, id)
                !unit.isWalking && free(scope, passage.exit) -> unit.path = listOf(passage.exit)
            }
        }
    }

    private fun finish(
        scope: RoomScope,
        state: RoomTransit,
        furniId: Int,
    ) {
        val passage = state.passages.remove(furniId) ?: return
        StateOut.oneWay(scope, furniId, inside = false)
        holds.release(passage.unit)
        if (scope.room.units[passage.unit.index] === passage.unit) walking.letThrough(scope, passage.unit, emptySet())
    }

    private fun free(
        scope: RoomScope,
        tile: Point,
    ): Boolean = WalkTiles.at(scope.room, tile).state == TileState.Open && !standsOn(scope, tile)

    private fun standsOn(
        scope: RoomScope,
        tile: Point,
    ): Boolean =
        scope.room.units
            .all()
            .any { it.tile == tile }

    private companion object {
        const val HALF_TURN = 4
    }
}
