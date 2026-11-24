package zel.room.furni.transit

import zel.room.RoomReads
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.room.tick.RoomTiming
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection
import zel.room.unit.UnitKind
import zel.room.unit.path.PathResult
import zel.room.unit.walk.Walking

/** Under the room lock. */
@Suppress("TooManyFunctions") // one per ride stage
class Rides(
    private val walking: Walking,
    private val holds: TransitHolds,
    private val hooks: TransitHooks,
    private val data: TransitData,
    private val reads: RoomReads = RoomReads.Inline,
) {
    fun useDoor(
        scope: RoomScope,
        state: RoomTransit,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        val anchor = furni.anchor ?: return
        if (unit.kind !is UnitKind.Player || holds.holds(unit)) return
        val front = anchor.step(furni.direction)
        when (unit.tile) {
            anchor -> {
                StateOut.data(scope, furni, OPEN)
                start(state, Ride(unit, RideKind.Door, furni.id, anchor, RideStage.Inside, scope.now() + STEP_MILLIS))
            }

            front -> {
                enterDoor(scope, state, furni, anchor, unit)
            }

            else -> {
                walkToFront(scope, state, furni, unit, front)
                return
            }
        }
    }

    fun useHopper(
        scope: RoomScope,
        state: RoomTransit,
        furni: RoomFurni,
        unit: RoomUnit,
        mayEnter: Boolean,
    ) {
        val anchor = furni.anchor ?: return
        if (unit.kind !is UnitKind.Player || holds.holds(unit)) return
        val front = anchor.step(furni.direction)
        if (unit.tile != front) return walkToFront(scope, state, furni, unit, front)
        if (!mayEnter || !hopperFree(state, furni)) return
        val room = scope.room
        val definitionId = furni.definition.id
        reads.read(scope, { data.otherHopper(definitionId, room.info.id) }) { inner, target ->
            val ready =
                target != null && inner.room === room && room.furni[furni.id] === furni && furni.anchor == anchor &&
                    inRoom(inner, unit) && unit.tile == front && !holds.holds(unit) && hopperFree(state, furni)
            if (ready) {
                StateOut.data(inner, furni, OPEN)
                val ride = Ride(unit, RideKind.Hopper, furni.id, anchor, RideStage.Jump, inner.now() + STEP_MILLIS)
                ride.destination = target
                start(state, ride)
            }
        }
    }

    private fun hopperFree(
        state: RoomTransit,
        furni: RoomFurni,
    ): Boolean = furni.state.ifEmpty { CLOSED } == CLOSED && !busy(state, furni.id)

    /** A step onto a tile teleport: the tile shows 1, 0, 2 a step apart, then the user is on its partner. */
    fun stepOnTile(
        scope: RoomScope,
        state: RoomTransit,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        val anchor = furni.anchor ?: return
        if (unit.kind !is UnitKind.Player || holds.holds(unit)) return
        state.rides.remove(unit.index)
        state.resets.removeAll { it.furniId == furni.id }
        unit.path = emptyList()
        StateOut.data(scope, furni, OPEN)
        start(state, Ride(unit, RideKind.Tile, furni.id, anchor, RideStage.Inside, scope.now() + STEP_MILLIS))
    }

    fun useTile(
        scope: RoomScope,
        state: RoomTransit,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        val anchor = furni.anchor ?: return
        if (unit.tile == anchor) stepOnTile(scope, state, furni, unit) else walking.walkTo(scope, unit, anchor)
    }

    fun arrive(
        scope: RoomScope,
        state: RoomTransit,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        val anchor = furni.anchor ?: return
        walking.arrivedByTransit(scope, unit)
        put(scope, unit, furni, anchor)
        if (furni.behaviour is TileTeleport) return landOnTile(scope, state, furni, anchor, unit)
        StateOut.whole(scope, furni, FLASH)
        start(state, Ride(unit, RideKind.Door, furni.id, anchor, RideStage.Exit, scope.now() + STEP_MILLIS))
    }

    /** A step off the arrival tile: during the stay the tile shows 1 and 0 at once, after it 0 a step later. */
    fun leftTile(
        scope: RoomScope,
        state: RoomTransit,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        val ride = state.rides[unit.index] ?: return
        if (ride.kind != RideKind.Tile || ride.furniId != furni.id) return
        when (ride.stage) {
            RideStage.Exit -> tileReset(scope, furni)
            RideStage.Leaving -> later(scope, state, furni, CLOSED, STEP_MILLIS)
            else -> return
        }
        state.rides.remove(unit.index)
    }

    /** Arrival on a tile teleport: shows 2 while occupied, 1 after [TILE_STAY_MILLIS], then the user steps off. */
    private fun landOnTile(
        scope: RoomScope,
        state: RoomTransit,
        furni: RoomFurni,
        anchor: Point,
        unit: RoomUnit,
    ) {
        StateOut.data(scope, furni, FLASH)
        val ride = Ride(unit, RideKind.Tile, furni.id, anchor, RideStage.Exit, scope.now() + TILE_STAY_MILLIS)
        state.pending.remove(unit.index)
        state.rides[unit.index] = ride
    }

    private fun tileReset(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        StateOut.data(scope, furni, OPEN)
        StateOut.data(scope, furni, CLOSED)
    }

    fun tick(
        scope: RoomScope,
        state: RoomTransit,
    ) {
        repeatUses(scope, state)
        state.rides.values.toList().forEach { ride ->
            when {
                !inRoom(scope, ride.unit) || !furniStays(scope, ride) -> abort(scope, state, ride)
                ride.resolving -> Unit
                ride.stage == RideStage.Entering -> entering(scope, state, ride)
                scope.now() + SLACK_MILLIS >= ride.due -> advance(scope, state, ride)
            }
        }
    }

    private fun enterDoor(
        scope: RoomScope,
        state: RoomTransit,
        furni: RoomFurni,
        anchor: Point,
        unit: RoomUnit,
    ) {
        StateOut.data(scope, furni, OPEN)
        walking.letThrough(scope, unit, setOf(furni.id))
        val walk = walking.walkTo(scope, unit, anchor)
        if (walk !is PathResult.Found || walk.path.isEmpty()) {
            walking.letThrough(scope, unit, emptySet())
            StateOut.data(scope, furni, CLOSED)
            return
        }
        start(state, Ride(unit, RideKind.Door, furni.id, anchor, RideStage.Entering, scope.now()))
    }

    private fun walkToFront(
        scope: RoomScope,
        state: RoomTransit,
        furni: RoomFurni,
        unit: RoomUnit,
        front: Point,
    ) {
        val walk = walking.walkTo(scope, unit, front)
        if (walk is PathResult.Found && walk.path.isNotEmpty()) state.pending[unit.index] = PendingUse(furni.id, front)
    }

    private fun repeatUses(
        scope: RoomScope,
        state: RoomTransit,
    ) {
        state.pending.entries.toList().forEach { (index, use) ->
            val unit = scope.room.units[index]
            val furni = scope.room.furni[use.furniId]
            if (unit != null && furni != null && unit.isWalking && unit.path.last() == use.front) return@forEach
            state.pending.remove(index)
            if (unit != null && furni != null && !unit.isWalking && unit.tile == use.front) {
                repeatUse(scope, state, furni, unit)
            }
        }
    }

    private fun repeatUse(
        scope: RoomScope,
        state: RoomTransit,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = when (val behaviour = furni.behaviour) {
        is DoorTeleport -> useDoor(scope, state, furni, unit)
        is Hopper -> useHopper(scope, state, furni, unit, behaviour.mayEnter(furni, unit))
        else -> Unit
    }

    private fun entering(
        scope: RoomScope,
        state: RoomTransit,
        ride: Ride,
    ) {
        val unit = ride.unit
        if (unit.isWalking) return
        if (unit.tile == ride.anchor) {
            ride.stage = RideStage.Inside
            ride.due = scope.now() + STEP_MILLIS
        } else {
            scope.room.furni[ride.furniId]?.let { StateOut.data(scope, it, CLOSED) }
            end(scope, state, ride)
        }
    }

    private fun advance(
        scope: RoomScope,
        state: RoomTransit,
        ride: Ride,
    ) {
        val furni = scope.room.furni[ride.furniId] ?: return end(scope, state, ride)
        if (ride.kind == RideKind.Tile) return advanceTile(scope, state, ride, furni)
        when (ride.stage) {
            RideStage.Entering, RideStage.Leaving -> Unit
            RideStage.Inside -> inside(scope, ride, furni)
            RideStage.Closing -> closing(scope, ride, furni)
            RideStage.Jump -> jump(scope, state, ride, furni)
            RideStage.Exit -> exit(scope, state, ride, furni)
        }
    }

    private fun advanceTile(
        scope: RoomScope,
        state: RoomTransit,
        ride: Ride,
        furni: RoomFurni,
    ) {
        when (ride.stage) {
            RideStage.Inside -> {
                StateOut.data(scope, furni, CLOSED)
                resolve(scope, ride, furni.id)
                next(scope, ride, RideStage.Closing)
            }

            RideStage.Closing -> {
                StateOut.data(scope, furni, FLASH)
                next(scope, ride, RideStage.Jump)
            }

            RideStage.Jump -> {
                later(scope, state, furni, CLOSED, STEP_MILLIS)
                tileJump(scope, state, ride)
            }

            RideStage.Exit -> {
                StateOut.data(scope, furni, OPEN)
                ride.stage = RideStage.Leaving
                ride.due = scope.now() + TILE_STAY_MILLIS
                walking.walkTo(scope, ride.unit, ride.anchor.step(furni.direction))
            }

            RideStage.Leaving -> {
                state.rides.remove(ride.unit.index)
                StateOut.data(scope, furni, CLOSED)
            }

            RideStage.Entering -> {
                end(scope, state, ride)
            }
        }
    }

    private fun inside(
        scope: RoomScope,
        ride: Ride,
        furni: RoomFurni,
    ) {
        ride.unit.face(Math.floorMod(furni.direction + HALF_TURN, UnitDirection.COUNT))
        next(scope, ride, RideStage.Closing)
    }

    private fun closing(
        scope: RoomScope,
        ride: Ride,
        furni: RoomFurni,
    ) {
        StateOut.whole(scope, furni, CLOSED)
        resolve(scope, ride, furni.id)
        next(scope, ride, RideStage.Jump)
    }

    private fun resolve(
        scope: RoomScope,
        ride: Ride,
        furniId: Int,
    ) {
        ride.resolving = true
        reads.read(scope, { data.partnerOf(furniId) }) { _, destination ->
            ride.destination = destination
            ride.resolving = false
        }
    }

    private fun jump(
        scope: RoomScope,
        state: RoomTransit,
        ride: Ride,
        furni: RoomFurni,
    ) = when (ride.kind) {
        RideKind.Door -> doorJump(scope, state, ride, furni)
        RideKind.Tile -> tileJump(scope, state, ride)
        RideKind.Hopper -> hopperJump(scope, state, ride, furni)
    }

    private fun doorJump(
        scope: RoomScope,
        state: RoomTransit,
        ride: Ride,
        source: RoomFurni,
    ) {
        val destination = ride.destination
        val partner = destination?.let { scope.room.furni[it.furniId] }?.takeIf { it.behaviour is DoorTeleport }
        val partnerAnchor = partner?.anchor
        when {
            partner != null && partnerAnchor != null -> {
                StateOut.data(scope, source, FLASH)
                later(scope, state, source, CLOSED, FLASH_MILLIS)
                StateOut.whole(scope, partner, FLASH)
                put(scope, ride.unit, partner, partnerAnchor)
                hooks.teleported(scope, ride.unit)
                ride.furniId = partner.id
                ride.anchor = partnerAnchor
                next(scope, ride, RideStage.Exit)
            }

            destination != null && elsewhere(scope, destination) -> {
                StateOut.data(scope, source, FLASH)
                later(scope, state, source, CLOSED, FLASH_MILLIS)
                hooks.teleported(scope, ride.unit)
                travelOrStay(scope, state, ride, destination, back = source)
            }

            else -> {
                exit(scope, state, ride, source)
            }
        }
    }

    private fun tileJump(
        scope: RoomScope,
        state: RoomTransit,
        ride: Ride,
    ) {
        val destination = ride.destination
        val partner = destination?.let { scope.room.furni[it.furniId] }?.takeIf { it.behaviour is TileTeleport }
        val partnerAnchor = partner?.anchor
        when {
            partner != null && partnerAnchor != null -> {
                put(scope, ride.unit, partner, partnerAnchor)
                hooks.teleported(scope, ride.unit)
                end(scope, state, ride)
                landOnTile(scope, state, partner, partnerAnchor, ride.unit)
            }

            destination != null && elsewhere(scope, destination) -> {
                hooks.teleported(scope, ride.unit)
                travelOrStay(scope, state, ride, destination, back = null)
            }

            else -> {
                end(scope, state, ride)
            }
        }
    }

    private fun hopperJump(
        scope: RoomScope,
        state: RoomTransit,
        ride: Ride,
        hopper: RoomFurni,
    ) {
        StateOut.data(scope, hopper, FLASH)
        later(scope, state, hopper, CLOSED, STEP_MILLIS)
        ride.destination?.let { hooks.travel(scope, ride.unit, it.roomId, it.furniId) }
        end(scope, state, ride)
    }

    private fun travelOrStay(
        scope: RoomScope,
        state: RoomTransit,
        ride: Ride,
        destination: Destination,
        back: RoomFurni?,
    ) {
        if (hooks.travel(scope, ride.unit, destination.roomId, destination.furniId) || back == null) {
            end(scope, state, ride)
        } else {
            exit(scope, state, ride, back)
        }
    }

    private fun exit(
        scope: RoomScope,
        state: RoomTransit,
        ride: Ride,
        furni: RoomFurni,
    ) {
        StateOut.whole(scope, furni, OPEN)
        later(scope, state, furni, CLOSED, FLASH_MILLIS)
        end(scope, state, ride)
        furni.anchor?.let { walking.walkTo(scope, ride.unit, it.step(furni.direction)) }
    }

    private fun put(
        scope: RoomScope,
        unit: RoomUnit,
        furni: RoomFurni,
        anchor: Point,
    ) {
        walking.place(scope, unit, anchor)
        unit.face(furni.direction)
        unit.changed = true
    }

    private fun start(
        state: RoomTransit,
        ride: Ride,
    ) {
        state.pending.remove(ride.unit.index)
        state.rides[ride.unit.index] = ride
        holds.hold(ride.unit)
    }

    private fun end(
        scope: RoomScope,
        state: RoomTransit,
        ride: Ride,
    ) {
        state.rides.remove(ride.unit.index)
        holds.release(ride.unit)
        if (inRoom(scope, ride.unit)) walking.letThrough(scope, ride.unit, emptySet())
    }

    private fun abort(
        scope: RoomScope,
        state: RoomTransit,
        ride: Ride,
    ) {
        end(scope, state, ride)
        val furni = scope.room.furni[ride.furniId] ?: return
        if (furni.state != CLOSED && !busy(state, furni.id)) StateOut.data(scope, furni, CLOSED)
    }

    private fun busy(
        state: RoomTransit,
        furniId: Int,
    ): Boolean = state.rides.values.any { it.furniId == furniId }

    private fun next(
        scope: RoomScope,
        ride: Ride,
        stage: RideStage,
    ) {
        ride.stage = stage
        ride.due = scope.now() + STEP_MILLIS
    }

    private fun later(
        scope: RoomScope,
        state: RoomTransit,
        furni: RoomFurni,
        to: String,
        afterMillis: Long,
    ) {
        state.resets.removeAll { it.furniId == furni.id }
        state.resets += StateReset(scope.now() + afterMillis, furni.id, to)
    }

    private fun inRoom(
        scope: RoomScope,
        unit: RoomUnit,
    ): Boolean = scope.room.units[unit.index] === unit

    private fun furniStays(
        scope: RoomScope,
        ride: Ride,
    ): Boolean = scope.room.furni[ride.furniId]?.anchor == ride.anchor

    private fun elsewhere(
        scope: RoomScope,
        destination: Destination,
    ): Boolean = destination.roomId > 0 && destination.roomId != scope.room.info.id

    companion object {
        const val CLOSED = "0"
        const val OPEN = "1"
        const val FLASH = "2"

        const val STEP_MILLIS = RoomTiming.TICK_MILLIS

        const val FLASH_MILLIS = 1_000L

        const val TILE_STAY_MILLIS = 1_000L

        const val SLACK_MILLIS = 50L

        private const val HALF_TURN = 4
    }
}
