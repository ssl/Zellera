package zel.wired.variable

import zel.game.items.FurniHeights
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection
import zel.room.unit.path.TileState
import zel.room.unit.path.WalkTiles
import zel.wired.effect.furni.FurniMoves
import zel.wired.firing.Firing
import zel.wired.firing.Movement
import zel.wired.timing.RoomPoster
import zel.wired.timing.markWiredMove

class UnitSlides(
    private val poster: RoomPoster,
    private val moves: FurniMoves,
) {
    fun slide(
        firing: Firing,
        unit: RoomUnit,
        tile: Point,
    ): Boolean {
        val room = firing.scope.room
        if (!room.floor.isFloor(tile)) return false
        val walk = WalkTiles.at(room, tile)
        if (walk.state == TileState.Blocked || walk.state == TileState.Void) return false
        val taken = room.units.all().any { it !== unit && it.tile == tile }
        if (taken) return false
        val from = Movement.Spot(unit.tile.x, unit.tile.y, FurniHeights.formatZ(unit.z))
        unit.tile = tile
        unit.z = walk.height
        unit.path = emptyList()
        firing.flash.movements.add(
            Movement.UnitMove(
                unit.index,
                from,
                Movement.Spot(tile.x, tile.y, FurniHeights.formatZ(unit.z)),
                Movement.UnitMove.SLIDE,
                moves.style(firing).duration,
                unit.bodyDirection,
                unit.headDirection,
            ),
        )
        mark(firing, unit)
        return true
    }

    /** Always a turn in the WiredMovements; a RoomUnitUpdate only after a real change. */
    fun turn(
        firing: Firing,
        unit: RoomUnit,
        direction: Int,
    ): Boolean {
        if (direction !in 0 until UnitDirection.COUNT) return false
        firing.flash.movements.add(Movement.UnitTurn(unit.index, direction))
        if (unit.bodyDirection != direction || unit.headDirection != direction) mark(firing, unit)
        unit.face(direction)
        return true
    }

    private fun mark(
        firing: Firing,
        unit: RoomUnit,
    ) = poster.markWiredMove(firing.scope, unit)
}
