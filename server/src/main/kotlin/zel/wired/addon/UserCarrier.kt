package zel.wired.addon

import zel.game.items.FurniHeights
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.walk.Walking
import zel.wired.firing.Firing
import zel.wired.firing.Movement
import zel.wired.timing.RoomPoster
import zel.wired.timing.markWiredMove
import zel.wired.timing.postureHeight

class UserCarrier(
    private val walking: Walking,
    private val poster: RoomPoster,
) {
    fun carry(
        firing: Firing,
        unit: RoomUnit,
        to: Point,
        duration: Int,
    ) {
        val scope = firing.scope
        val from = unit.tile
        // As in a lift: the entry's z carries the posture height.
        val fromZ = unit.z + unit.postureHeight
        walking.movedByWired(scope, unit)
        walking.place(scope, unit, to)
        firing.flash.movements.add(
            Movement.UnitMove(
                unit.index,
                Movement.Spot(from.x, from.y, FurniHeights.formatZ(fromZ)),
                Movement.Spot(to.x, to.y, FurniHeights.formatZ(unit.z + unit.postureHeight)),
                Movement.UnitMove.SLIDE,
                duration,
                unit.bodyDirection,
                unit.headDirection,
            ),
        )
        poster.markWiredMove(scope, unit)
    }
}
