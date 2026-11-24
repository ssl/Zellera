package zel.room.unit.walk

import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.UnitStatus
import zel.room.unit.path.PathResult
import zel.room.unit.path.PathSearch
import zel.room.unit.path.TileState
import zel.room.unit.path.WalkGrid
import zel.room.unit.path.WalkTiles

class WalkRequests(
    private val hooks: WalkHooks,
    private val search: PathSearch = PathSearch(),
) {
    fun request(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
        clicked: Point,
    ) {
        if (walk.sentOut || walk.doorStep || !hooks.mayWalk(unit)) return
        if (!FloodGuard.accept(walk, scope.now(), clicked)) return
        val target = walk.refusedGoal ?: clicked
        walk.refusedGoal = null
        clickTiles(scope, unit, target)
        // the click's own wired (a freeze) can stop this walk
        if (target == unit.tile || !hooks.mayWalk(unit)) return
        hooks.active(scope, unit)
        if (unit.showsStatus(UnitStatus.LAY) && staysInBed(scope, unit, walk, target)) return
        val goal = freePillow(scope, unit, walk, target) ?: target
        walk.leaving = false
        walk.sentOut = false
        walk.leaveAt = null
        walkTo(scope, unit, walk, goal)
    }

    fun walkTo(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
        goal: Point,
    ): PathResult {
        if (unit.kind is UnitKind.Bot && goal == scope.room.floor.door.tile) return PathResult.GoalRefused
        val result = find(scope, unit, walk, goal)
        if (result != PathResult.GoalRefused) walk.wiredWalk = false
        when (result) {
            PathResult.GoalRefused -> Unit
            PathResult.NoPath -> stop(scope, unit, walk)
            is PathResult.Found -> start(scope, unit, walk, result.path)
        }
        return result
    }

    /** The steps of the walk [walkTo] would give [unit] from where it stands to [goal]; null when none. */
    fun stepsTo(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
        goal: Point,
    ): Int? = (find(scope, unit, walk, goal) as? PathResult.Found)?.path?.size

    private fun find(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
        goal: Point,
    ): PathResult {
        val fromZ = unit.z - if (walk.rides != null) RIDE_HEIGHT else 0.0
        return search.find(WalkGrid.of(scope.room), unit.tile, goal, Movers.of(unit, walk, hooks), fromZ)
    }

    private fun start(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
        path: List<Point>,
    ) {
        if (unit.isWalking) endWalk(scope, unit, walk)
        unit.path = path
    }

    private fun stop(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
    ) {
        if (unit.isWalking) endWalk(scope, unit, walk)
        unit.path = emptyList()
    }

    private fun endWalk(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
    ) {
        if (walk.steps > 0) hooks.walkEnded(scope, unit, walk.steps)
        walk.steps = 0
    }

    private fun clickTiles(
        scope: RoomScope,
        unit: RoomUnit,
        target: Point,
    ) {
        scope.room.furni
            .at(target)
            .furni
            .filter {
                it.definition.interaction.name == CLICK_TILE
            }.forEach { scope.raise(RoomEvent.ClickTile(unit.index, it.id)) }
    }

    private fun staysInBed(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
        target: Point,
    ): Boolean {
        val bed = (WalkTiles.at(scope.room, unit.tile).state as? TileState.Bed)?.furni
        if (bed != null && target in bed.tiles() && Beds.pillowFor(bed, target) == unit.tile) {
            if (unit.isWalking) stop(scope, unit, walk)
            walk.stayLines = UnitLines.STAY_WITH_SLOT
            unit.changed = true
            return true
        }
        // A click on the two tiles in front of the bed is taken as a click at the bed, not as a walk.
        val front = unit.tile.step(unit.bodyDirection)
        return target == front || target == front.step(unit.bodyDirection)
    }

    private fun freePillow(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
        target: Point,
    ): Point? {
        val bed = (WalkTiles.at(scope.room, target).state as? TileState.Bed)?.furni ?: return null
        if (walk.rides != null || walk.riddenBy != null) return null
        return (listOf(Beds.pillowFor(bed, target)) + Beds.pillows(bed)).firstOrNull { !holdsOther(scope, it, unit) }
    }

    private fun holdsOther(
        scope: RoomScope,
        tile: Point,
        unit: RoomUnit,
    ): Boolean =
        scope.room.units
            .all()
            .any { it !== unit && it.tile == tile }

    companion object {
        const val RIDE_HEIGHT = 1.0

        private const val CLICK_TILE = "tile.click"
    }
}
