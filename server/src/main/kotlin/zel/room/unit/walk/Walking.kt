package zel.room.unit.walk

import zel.game.items.FurniHeights
import zel.protocol.generated.RoomUnitUpdateOut
import zel.protocol.generated.RoomUnitUpdateOutCodec
import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.map.Point
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.tick.TickPart
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.UnitStatus
import zel.room.unit.path.PathResult
import zel.room.unit.path.PathSearch
import zel.room.unit.path.TileState
import zel.room.unit.path.WalkTiles
import zel.room.unit.walk.WalkRequests.Companion.RIDE_HEIGHT
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

@Suppress("TooManyFunctions") // the walking API every room part calls
class Walking(
    next: WalkHooks = WalkHooks.None,
    /** Runs after the units' steps (rollers), before the tick's RoomUnitUpdate. */
    afterSteps: (RoomScope, Walking) -> Unit = { _, _ -> },
    publicDoorLets: () -> Boolean = { false },
    stepLimits: () -> PathSearch.StepLimits = { PathSearch.StepLimits() },
) : RoomPart {
    /** Rooms' walk state; each entry is read and written only under its room's lock. */
    private val rooms = ConcurrentHashMap<Room, RoomWalks>()
    val walkedTiles = WalkedTiles(next)
    private val hooks: WalkHooks = walkedTiles
    private val search = PathSearch(stepLimits = stepLimits)
    private val requests = WalkRequests(hooks, search)
    private val postures = Postures(hooks)
    private val steps = UnitSteps(hooks, postures, publicDoorLets, { stepLimits().maxUp }, search)

    val tick: TickPart =
        TickPart { scope ->
            val walks = walksOf(scope.room)
            walks.prune(scope.room.units)
            scope.room.units
                .all()
                .forEach { walks.of(it).movedByWired = false }
            steps.tick(scope, walks)
            afterSteps(scope, this)
            sendChanged(scope)
        }

    override fun load(room: Room) {
        rooms[room] = RoomWalks()
    }

    override fun unload(scope: RoomScope) {
        rooms.remove(scope.room)
    }

    fun request(
        scope: RoomScope,
        unit: RoomUnit,
        clicked: Point,
    ) {
        if (hooks.takesClick(scope, unit, clicked)) return
        requests.request(scope, unit, walk(scope, unit), clicked)
    }

    /**
     * A bot or pet held still (a wired freeze, a game) starts no walk; a held player's are refused on request.
     * [byWired]: the walk comes from a wired box (bot_move), not from the unit's own stroll or errand.
     */
    fun walkTo(
        scope: RoomScope,
        unit: RoomUnit,
        goal: Point,
        byWired: Boolean = false,
    ): PathResult {
        if (unit.kind !is UnitKind.Player && !hooks.mayWalk(unit)) return PathResult.GoalRefused
        val walk = walk(scope, unit)
        val result = requests.walkTo(scope, unit, walk, goal)
        if (byWired && result is PathResult.Found) walk.wiredWalk = unit.isWalking
        return result
    }

    /** The steps of the walk [walkTo] would give [unit] from where it stands to [goal]; null when it finds none. */
    fun stepsTo(
        scope: RoomScope,
        unit: RoomUnit,
        goal: Point,
    ): Int? = requests.stepsTo(scope, unit, walk(scope, unit), goal)

    /** The unit walks a path a wired box gave it (bot_move), see [walkTo]. */
    fun walksByWired(
        scope: RoomScope,
        unit: RoomUnit,
    ): Boolean = unit.isWalking && walk(scope, unit).wiredWalk

    fun changePosture(
        scope: RoomScope,
        unit: RoomUnit,
        posture: Int,
    ) {
        if (postures.change(scope, unit, walk(scope, unit), posture)) sendNow(scope, listOf(unit))
    }

    fun lookTo(
        unit: RoomUnit,
        point: Point,
    ) = postures.lookTo(unit, point)

    fun turnToClicked(
        unit: RoomUnit,
        point: Point,
    ) = postures.turnToClicked(unit, point)

    fun quit(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        if (!walk(scope, unit).sentOut) walkToDoor(scope, unit, sentOut = false)
    }

    fun sendOut(
        scope: RoomScope,
        unit: RoomUnit,
    ) = walkToDoor(scope, unit, sentOut = true)

    private fun walkToDoor(
        scope: RoomScope,
        unit: RoomUnit,
        sentOut: Boolean,
    ) {
        val walk = walk(scope, unit)
        val door = scope.room.floor.door.tile
        val result = if (unit.tile == door) null else requests.walkTo(scope, unit, walk, door)
        if (result is PathResult.Found && result.path.isNotEmpty()) {
            walk.leaving = true
            walk.sentOut = sentOut
        } else {
            hooks.leave(scope, unit)
        }
    }

    /**
     * Puts the units standing on [tiles] at the tiles' new walk height. With [lifts] false (wired furni moves) a
     * unit is only lowered when its support went away; a unit carried along this tick waits for its wired-move line.
     * A seat or bed moved under a standing unit settles it on the next unit tick ([UnitWalk.settleOnTick]). On [onto]
     * (tiles a wired move landed a furni on) a standing unit is also raised when it can stand there.
     */
    fun furniChanged(
        scope: RoomScope,
        tiles: Collection<Point>,
        lifts: Boolean = true,
        onto: Collection<Point> = emptyList(),
    ) {
        val touched =
            scope.room.units
                .all()
                .filter { it.tile in tiles && !it.isWalking }
        val onTick = mutableSetOf<RoomUnit>()
        touched.forEach { unit ->
            val walk = walk(scope, unit)
            val tile = WalkTiles.at(scope.room, unit.tile)
            if (!lifts && settlesOnTick(unit, walk, tile.state)) {
                walk.settleOnTick = true
                return@forEach
            }
            postures.settle(scope, unit, walk)
            val standsOn = tile.state == TileState.Open || tile.state is TileState.Restricted
            val z = tile.height + if (walk.rides != null) RIDE_HEIGHT else 0.0
            val raises = lifts || unit.tile in onto
            if (standsOn && abs(unit.z - z) > HEIGHT_EPSILON && (raises || z < unit.z)) {
                if (!lifts && z > unit.z) onTick += unit
                unit.z = z
                unit.changed = true
            }
        }
        sendNow(
            scope,
            touched.filter {
                val walk = walk(scope, it)
                it.changed && (lifts || !walk.movedByWired) && !walk.settleOnTick && it !in onTick
            },
        )
    }

    /** A standing unit, neither riding nor ridden, under a seat or bed. */
    private fun settlesOnTick(
        unit: RoomUnit,
        walk: UnitWalk,
        state: TileState,
    ): Boolean =
        (state is TileState.Seat || state is TileState.Bed) &&
            walk.rides == null &&
            walk.riddenBy == null &&
            !unit.showsStatus(UnitStatus.SIT) &&
            !unit.showsStatus(UnitStatus.LAY)

    /** Puts [unit] on [tile] at its walk height, or at [z] when given. */
    fun place(
        scope: RoomScope,
        unit: RoomUnit,
        tile: Point,
        z: Double? = null,
    ) {
        val walk = walk(scope, unit)
        unit.path = emptyList()
        unit.tile = tile
        unit.z = (z ?: WalkTiles.at(scope.room, tile).height) + if (walk.rides != null) RIDE_HEIGHT else 0.0
        unit.changed = true
        postures.settle(scope, unit, walk)
    }

    /** A seat with no seat furni under it; it holds until [unseat]. */
    fun seat(
        scope: RoomScope,
        unit: RoomUnit,
        height: Double,
        direction: Int,
    ) {
        val walk = walk(scope, unit)
        unit.path = emptyList()
        unit.dropStatus(UnitStatus.MOVE)
        unit.dropStatus(UnitStatus.LAY)
        walk.floorSit = true
        unit.face(direction)
        unit.setStatus(UnitStatus.SIT, "${FurniHeights.formatHeight(height)} 0")
        unit.changed = true
        sendNow(scope, listOf(unit))
    }

    fun unseat(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        walk(scope, unit).floorSit = false
        unit.dropStatus(UnitStatus.SIT)
        unit.changed = true
    }

    /** Sits or lays a unit put down on a seat or bed without walking there (bots and pets at room load). */
    fun rest(
        room: Room,
        unit: RoomUnit,
    ) = postures.rest(room, unit, walksOf(room).of(unit))

    /**
     * Ends [unit]'s walk where it stands (a step under way still finishes) and gives its next line the empty move
     * slot, the line after it none: a wired teleport's line on the tile it leaves, then its landing.
     */
    fun stopWithEmptySlot(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        val walk = walk(scope, unit)
        if (unit.path.isNotEmpty()) steps.endWalk(scope, unit, walk)
        walk.stayLines = UnitLines.STAY_WITH_SLOT
        unit.changed = true
    }

    /**
     * Plans [unit]'s walk to [goal] again from the tile a wired teleport just put it on: a walk asked between the
     * teleport's line and its landing goes on, the landing line carrying the first step.
     */
    fun resumeAfterTeleport(
        scope: RoomScope,
        unit: RoomUnit,
        goal: Point,
    ) {
        if (!hooks.mayWalk(unit)) return
        val walk = walk(scope, unit)
        walk.stayLines = 0
        requests.walkTo(scope, unit, walk, goal)
    }

    /** [unit]'s wired-teleport line or landing ([stopWithEmptySlot]) is still to be sent. */
    fun teleportLinePending(
        scope: RoomScope,
        unit: RoomUnit,
    ): Boolean = walk(scope, unit).stayLines > 0

    fun movedByWired(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        val walk = walk(scope, unit)
        walk.movedByWired = true
        walk.stepOrigin?.let { walk.stepLine = UnitLines.stepLine(unit, it) }
    }

    fun stepOrigin(
        scope: RoomScope,
        unit: RoomUnit,
    ): StepOrigin? = walk(scope, unit).stepOrigin

    fun arrivedByTransit(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        walk(scope, unit).cameByTransit = true
    }

    fun letThrough(
        scope: RoomScope,
        unit: RoomUnit,
        furniIds: Set<Int>,
    ) {
        walk(scope, unit).letThrough = furniIds
    }

    fun ride(
        scope: RoomScope,
        rider: RoomUnit,
        horse: RoomUnit,
    ) {
        val riderWalk = walk(scope, rider)
        riderWalk.rides = horse.index
        walk(scope, horse).riddenBy = rider.index
        horse.path = emptyList()
        rider.path = emptyList()
        rider.tile = horse.tile
        rider.z = horse.z + RIDE_HEIGHT
        rider.face(horse.bodyDirection)
        postures.settle(scope, rider, riderWalk)
        rider.changed = true
    }

    fun horseOf(
        scope: RoomScope,
        rider: RoomUnit,
    ): RoomUnit? = walk(scope, rider).rides?.let { scope.room.units[it] }

    fun isRidden(
        scope: RoomScope,
        horse: RoomUnit,
    ): Boolean = walk(scope, horse).riddenBy != null

    fun dismount(
        scope: RoomScope,
        rider: RoomUnit,
    ) {
        val riderWalk = walk(scope, rider)
        val horseIndex = riderWalk.rides ?: return
        riderWalk.rides = null
        scope.room.units[horseIndex]?.let { walk(scope, it).riddenBy = null }
        rider.z -= RIDE_HEIGHT
        rider.changed = true
    }

    fun riderGone(
        scope: RoomScope,
        horse: RoomUnit,
    ) {
        walk(scope, horse).riddenBy = null
        horse.path = emptyList()
        horse.changed = true
    }

    fun sendNow(
        scope: RoomScope,
        units: List<RoomUnit>,
    ) {
        if (units.isEmpty()) return
        val walks = walksOf(scope.room)
        val lines = units.map { lineOf(it, walks.of(it)) }
        scope.out.send(Lane.Now, Audience.Room, RoomUnitUpdateOutCodec, RoomUnitUpdateOut(lines))
    }

    private fun sendChanged(scope: RoomScope) {
        val walks = walksOf(scope.room)
        sendNow(
            scope,
            scope.room.units
                .all()
                .filter { (it.changed && !walks.of(it).movedByWired) || walks.of(it).stepLine != null },
        )
    }

    private fun lineOf(
        unit: RoomUnit,
        walk: UnitWalk,
    ): RoomUnitUpdateOut.List1Item {
        walk.stepLine?.let {
            walk.stepLine = null
            return it
        }
        val line = UnitLines.line(unit, walk)
        unit.changed = false
        walk.shownTile = null
        if (walk.stayLines > 0) {
            walk.stayLines--
            if (walk.stayLines > 0) unit.changed = true
        }
        return line
    }

    private fun walk(
        scope: RoomScope,
        unit: RoomUnit,
    ): UnitWalk = walksOf(scope.room).of(unit)

    private fun walksOf(room: Room): RoomWalks = rooms.getOrPut(room) { RoomWalks() }

    private companion object {
        const val HEIGHT_EPSILON = 1e-6
    }
}
