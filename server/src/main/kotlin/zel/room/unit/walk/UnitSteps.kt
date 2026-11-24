package zel.room.unit.walk

import zel.game.items.FurniHeights
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.Steppable
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection
import zel.room.unit.UnitKind
import zel.room.unit.UnitStatus
import zel.room.unit.path.Mover
import zel.room.unit.path.ObstacleBar
import zel.room.unit.path.Passage
import zel.room.unit.path.PathResult
import zel.room.unit.path.PathSearch
import zel.room.unit.path.TileState
import zel.room.unit.path.WalkGrid
import zel.room.unit.path.WalkTiles
import zel.room.unit.walk.WalkRequests.Companion.RIDE_HEIGHT

class UnitSteps(
    private val hooks: WalkHooks,
    private val postures: Postures,
    private val publicDoorLets: () -> Boolean = { false },
    private val maxStepUp: () -> Double = { PathSearch.MAX_STEP_UP },
    private val search: PathSearch = PathSearch(),
) {
    fun tick(
        scope: RoomScope,
        walks: RoomWalks,
    ) {
        stepOrder(scope).forEach { unit ->
            val walk = walks.of(unit)
            val settles = walk.settleOnTick
            walk.settleOnTick = false
            if (walk.riddenBy == null) unitTick(scope, unit, walk, walks, settles)
        }
    }

    /** Players step before bots and bots before pets; within a kind in the order of entry. */
    private fun stepOrder(scope: RoomScope): List<RoomUnit> =
        scope.room.units.all().sortedBy {
            when (it.kind) {
                is UnitKind.Player -> 0
                is UnitKind.Bot -> 1
                is UnitKind.Pet -> 2
            }
        }

    fun endWalk(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
    ) {
        unit.path = emptyList()
        if (walk.steps > 0) hooks.walkEnded(scope, unit, walk.steps)
        walk.steps = 0
        if (walk.sentOut) walk.leaveAt = scope.now() + LEAVE_DELAY_MILLIS
        walk.leaving = walk.sentOut
    }

    private fun unitTick(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
        walks: RoomWalks,
        settles: Boolean,
    ) {
        val horse = walk.rides?.let { scope.room.units[it] }
        val nothingPending = !unit.changed
        val arrived = unit.showsStatus(UnitStatus.MOVE)
        unit.dropStatus(UnitStatus.MOVE)
        horse?.dropStatus(UnitStatus.MOVE)
        if (leavesNow(scope, unit, walk)) return
        when {
            unit.isWalking -> step(scope, unit, walk, horse?.let { it to walks.of(it) }, nothingPending)
            arrived && quitsOnDoor(scope, unit, walk) -> leaveOnArrival(scope, unit, walk)
            arrived -> arrive(scope, unit, walk)
            settles -> postures.settle(scope, unit, walk)
            else -> postures.keep(scope, unit, walk)
        }
        if (horse != null && !unit.isWalking) keepUnder(unit, horse)
    }

    /** A Quit walk (not a sent-out one) whose last step reached the room's door. */
    private fun quitsOnDoor(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
    ): Boolean = walk.leaving && !walk.sentOut && unit.tile == scope.room.floor.door.tile

    /**
     * A Quit walk leaves on the tick after its door step, with no line for standing on the door: the last line is
     * the door step itself, then YouArePlayingGame false and CloseConnection together on the next tick.
     */
    private fun leaveOnArrival(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
    ) {
        walk.leaving = false
        unit.changed = false
        hooks.leave(scope, unit)
    }

    private fun leavesNow(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
    ): Boolean {
        val at = walk.leaveAt ?: return false
        if (scope.now() + LEAVE_SLACK_MILLIS < at) return false
        walk.leaveAt = null
        walk.leaving = false
        walk.sentOut = false
        if (walk.doorStep) hooks.walkEnded(scope, unit, 1)
        walk.doorStep = false
        hooks.leave(scope, unit)
        return true
    }

    private fun step(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
        horse: Pair<RoomUnit, UnitWalk>?,
        nothingPending: Boolean,
    ) {
        val room = scope.room
        // Stepping off a chair, a bed or a floor sit is standing up: the stand (7) action fires with the first step.
        val rises = unit.showsStatus(UnitStatus.SIT) || unit.showsStatus(UnitStatus.LAY)
        unit.dropStepPostures()
        walk.floorSit = false
        if (rises) scope.raise(RoomEvent.Action(unit.index, Postures.ACTION_STAND, ""))
        val mover = Movers.of(unit, walk, hooks)
        val grid = WalkGrid.of(room)
        if (!mayStep(grid, unit, walk, mover, unit.path.first())) routeAroundUnit(grid, unit, walk, mover)
        val next = unit.path.first()
        val direction = UnitDirection.between(unit.tile, next)
        if (direction == null || !mayStep(grid, unit, walk, mover, next)) {
            endWalk(scope, unit, walk)
            return
        }
        if (leavesBeforeDoor(scope, unit, walk, next, nothingPending) || gateRefuses(scope, unit, walk, next)) return
        val from = unit.tile
        val fromZ = unit.z
        val ride = if (walk.rides != null) RIDE_HEIGHT else 0.0
        val z = grid.tile(next).height + ride
        val toTop = room.furni.at(next).stepItem()
        unit.face(direction)
        walk.stepOrigin = StepOrigin(from, fromZ, next, z)
        val stepped = crossFurni(scope, unit, walk, next, z, room.furni.at(from).stepItem(), toTop)
        walk.stepOrigin = null
        if (!stepped) return
        unit.path = unit.path.drop(1)
        unit.setStatus(UnitStatus.MOVE, move(next, z))
        walk.shownTile = from
        walk.shownZ = fromZ
        walk.steps++
        horse?.let { (h, hw) -> follow(h, hw, next, z - RIDE_HEIGHT, direction) }
        if (horse != null) obstacleStep(scope, unit, from, next, direction)
        railStep(scope, unit, walk, mover, next)
        hooks.stepped(scope, unit, next)
        slideOnRail(scope, unit, walk, mover, next, direction)
        if (!unit.isWalking) {
            hooks.walkEnded(scope, unit, walk.steps)
            walk.steps = 0
        }
    }

    @Suppress("LongParameterList") // the step's state, passed so the step needs no extra object
    private fun crossFurni(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
        next: Point,
        z: Double,
        fromTop: RoomFurni?,
        toTop: RoomFurni?,
    ): Boolean {
        if (fromTop != null && fromTop !== toTop) {
            (fromTop.behaviour as? Steppable)?.onStepOff(scope, fromTop, unit)
            scope.raise(RoomEvent.StepOff(unit.index, fromTop.id))
        }
        if (walk.movedByWired) return false
        unit.tile = next
        unit.z = z
        if (toTop != null && toTop !== fromTop) {
            (toTop.behaviour as? Steppable)?.onStepOn(scope, toTop, unit)
            scope.raise(RoomEvent.StepOn(unit.index, toTop.id))
        }
        if (unit.kind is UnitKind.Bot) {
            scope.room.furni
                .at(next)
                .furni
                .forEach { scope.raise(RoomEvent.BotReachedFurni(unit.index, it.id)) }
        }
        return !walk.movedByWired
    }

    private fun gateRefuses(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
        next: Point,
    ): Boolean {
        val gateTop =
            scope.room.furni
                .at(next)
                .topItem()
        val gate = gateTop?.behaviour as? StepGate
        if (gateTop == null || gate == null || gate.lets(scope, gateTop, unit)) return false
        endWalk(scope, unit, walk)
        unit.changed = true
        gate.refused(scope, gateTop, unit)
        return true
    }

    /**
     * A user's own walk whose last step is the door tile ends before it, unseen: the user leaves 0.5 s later and the
     * door tile is counted. A Quit walk ([leaveOnArrival]) and a sent-out walk ([arrive]) do step onto the door.
     */
    private fun leavesBeforeDoor(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
        next: Point,
        nothingPending: Boolean,
    ): Boolean {
        if (walk.leaving || unit.path.size != 1 || !leavesFrom(scope, unit, walk, next)) return false
        unit.path = emptyList()
        if (nothingPending) unit.changed = false
        if (walk.steps > 0) hooks.walkEnded(scope, unit, walk.steps)
        walk.steps = 0
        walk.doorStep = true
        walk.leaveAt = scope.now() + LEAVE_DELAY_MILLIS
        hooks.leavingByDoor(scope, unit)
        return true
    }

    /** [tile] is the room's door, the door lets users out and its top furni is no teleporter / room linker. */
    private fun leavesFrom(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
        tile: Point,
    ): Boolean {
        val doorLets = !scope.room.info.isPublic || publicDoorLets()
        if (!doorLets || unit.kind !is UnitKind.Player || tile != scope.room.floor.door.tile) return false
        if (walk.cameByTransit) return false
        val top =
            scope.room.furni
                .at(tile)
                .topItem()
        return top
            ?.definition
            ?.interaction
            ?.name
            ?.lowercase() !in TRANSIT
    }

    private fun routeAroundUnit(
        grid: WalkGrid,
        unit: RoomUnit,
        walk: UnitWalk,
        mover: Mover,
    ) {
        val next = unit.path.first()
        if (!grid.holdsUnit(next, mover)) return
        if (unit.path.size == 1) {
            if (unit.kind is UnitKind.Player && !grid.nearDoor(next)) {
                walk.stayLines = UnitLines.STAY_WITH_SLOT
                unit.changed = true
            }
            return
        }
        val standing = unit.z - if (walk.rides != null) RIDE_HEIGHT else 0.0
        val found = search.find(grid, unit.tile, unit.path.last(), mover, standing) as? PathResult.Found ?: return
        if (found.path.isNotEmpty()) unit.path = found.path
    }

    private fun mayStep(
        grid: WalkGrid,
        unit: RoomUnit,
        walk: UnitWalk,
        mover: Mover,
        next: Point,
    ): Boolean {
        val last = unit.path.size == 1
        val tile = grid.tile(next)
        val open =
            when (val state = tile.state) {
                TileState.Open -> true
                is TileState.Restricted -> mover.mayUse(state.passage, asGoal = last)
                is TileState.Seat, is TileState.Bed -> last
                TileState.Void, TileState.Blocked -> false
            }
        val standing = unit.z - if (walk.rides != null) RIDE_HEIGHT else 0.0
        if (!open || tile.height - standing > maxStepUp() + EPSILON) return false
        if (!grid.holdsUnit(next, mover) || tile.state.isTransit()) return true
        return !last && (grid.walkThrough || grid.nearDoor(next))
    }

    private fun obstacleStep(
        scope: RoomScope,
        rider: RoomUnit,
        from: Point,
        onto: Point,
        direction: Int,
    ) {
        val room = scope.room
        val entered = ObstacleBar.entered(room, from, onto)
        if (entered != null && !UnitDirection.isDiagonal(direction)) {
            val bar = ObstacleBar.middle(entered)
            val (jumped, free) = rider.path.partition { it in bar }
            if (jumped.isNotEmpty() && free.isNotEmpty()) {
                rider.path = free
                hooks.horseJumped(scope, rider, entered)
                return
            }
        }
        ObstacleBar.crossed(room, from, onto)?.let { hooks.horseOnBar(scope, rider, it) }
    }

    private fun railStep(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
        mover: Mover,
        onto: Point,
    ) {
        walk.railSteps = if (mover.skater && WalkGrid.of(scope.room).isRail(onto)) walk.railSteps + 1 else 0
        if (walk.railSteps > 0) hooks.railStep(scope, unit, walk.railSteps)
    }

    @Suppress("LongParameterList") // the step's state, passed so the step needs no extra object
    private fun slideOnRail(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
        mover: Mover,
        rail: Point,
        direction: Int,
    ) {
        if (!mover.skater) return
        val grid = WalkGrid.of(scope.room)
        if (!grid.isRail(rail)) return
        if (unit.isWalking && !grid.isRail(rail.step(direction))) return
        val slide = slideTiles(grid, unit, walk, mover, rail, direction)
        if (slide.isNotEmpty() && unit.path.take(slide.size) != slide) unit.path = slide
    }

    @Suppress("LongParameterList") // the step's state, passed so the step needs no extra object
    private fun slideTiles(
        grid: WalkGrid,
        unit: RoomUnit,
        walk: UnitWalk,
        mover: Mover,
        rail: Point,
        direction: Int,
    ): List<Point> {
        val tiles = ArrayList<Point>()
        var height = unit.z - if (walk.rides != null) RIDE_HEIGHT else 0.0
        var ahead = rail.step(direction)
        while (grid.isRail(ahead) || grid.tile(ahead).state == TileState.Open) {
            val rises = grid.tile(ahead).height - height > maxStepUp() + EPSILON
            if (grid.holdsUnit(ahead, mover) || rises) break
            tiles += ahead
            if (!grid.isRail(ahead)) break
            height = grid.tile(ahead).height
            ahead = ahead.step(direction)
        }
        return tiles
    }

    private fun WalkGrid.isRail(tile: Point): Boolean =
        (tile(tile).state as? TileState.Restricted)?.passage == Passage.SkateRail

    private fun arrive(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
    ) {
        postures.settle(scope, unit, walk)
        takeWalkHeight(scope, unit, walk)
        if (walk.leaving || leavesFrom(scope, unit, walk, unit.tile)) walk.leaveAt = scope.now() + LEAVE_DELAY_MILLIS
    }

    private fun takeWalkHeight(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
    ) {
        if (walk.rides != null) return
        val tile = WalkTiles.at(scope.room, unit.tile)
        if (tile.state == TileState.Open || tile.state is TileState.Restricted) unit.z = tile.height
    }

    private fun follow(
        horse: RoomUnit,
        horseWalk: UnitWalk,
        to: Point,
        z: Double,
        direction: Int,
    ) {
        horseWalk.shownTile = horse.tile
        horseWalk.shownZ = horse.z
        horse.path = emptyList()
        horse.tile = to
        horse.z = z
        horse.face(direction)
        horse.setStatus(UnitStatus.MOVE, move(to, z))
    }

    private fun keepUnder(
        rider: RoomUnit,
        horse: RoomUnit,
    ) {
        if (horse.tile == rider.tile) return
        horse.tile = rider.tile
        horse.z = rider.z - RIDE_HEIGHT
        horse.changed = true
    }

    private fun move(
        to: Point,
        z: Double,
    ): String = "${to.x},${to.y},${FurniHeights.formatZ(z)}"

    companion object {
        const val LEAVE_DELAY_MILLIS = 500L

        private const val LEAVE_SLACK_MILLIS = 50L
        private const val EPSILON = 1e-4
        private val TRANSIT = setOf("transit.door", "transit.tile", "wf_room_linker")
    }
}
