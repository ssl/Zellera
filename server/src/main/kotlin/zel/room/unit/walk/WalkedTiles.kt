package zel.room.unit.walk

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind

class WalkedTiles(
    private val next: WalkHooks,
) : WalkHooks by next {
    var count: (playerId: Int, tiles: Int) -> Unit = { _, _ -> }

    var jumped: (scope: RoomScope, rider: RoomUnit, obstacle: RoomFurni) -> Unit = { _, _, _ -> }

    var onBar: (scope: RoomScope, rider: RoomUnit, obstacle: RoomFurni) -> Unit = { _, _, _ -> }

    var inTheAir: (RoomUnit) -> Boolean = { false }

    var railStepped: (playerId: Int, effect: Int, inARow: Int) -> Unit = { _, _, _ -> }

    override fun mayWalk(unit: RoomUnit): Boolean = !inTheAir(unit) && next.mayWalk(unit)

    override fun horseJumped(
        scope: RoomScope,
        rider: RoomUnit,
        obstacle: RoomFurni,
    ) {
        jumped(scope, rider, obstacle)
        next.horseJumped(scope, rider, obstacle)
    }

    override fun horseOnBar(
        scope: RoomScope,
        rider: RoomUnit,
        obstacle: RoomFurni,
    ) {
        onBar(scope, rider, obstacle)
        next.horseOnBar(scope, rider, obstacle)
    }

    override fun railStep(
        scope: RoomScope,
        unit: RoomUnit,
        inARow: Int,
    ) {
        (unit.kind as? UnitKind.Player)?.let { railStepped(it.playerId, next.effect(unit), inARow) }
        next.railStep(scope, unit, inARow)
    }

    override fun walkEnded(
        scope: RoomScope,
        unit: RoomUnit,
        steps: Int,
    ) {
        (unit.kind as? UnitKind.Player)?.let { count(it.playerId, steps) }
        next.walkEnded(scope, unit, steps)
    }
}
