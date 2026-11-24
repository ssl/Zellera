package zel.wired.effect.user

import zel.game.items.FurniHeights
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.RoomHeights
import zel.room.furni.Steppable
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.UnitStatus
import zel.room.unit.path.TileState
import zel.room.unit.path.WalkTiles
import zel.room.unit.walk.Walking
import zel.wired.WiredRooms
import zel.wired.addon.MoveStyles
import zel.wired.effect.furni.MoveAddon
import zel.wired.effect.furni.MoveThrough
import zel.wired.firing.Firing
import zel.wired.firing.Movement
import zel.wired.timing.RoomPoster
import zel.wired.timing.markWiredMove
import zel.wired.timing.postureHeight

class UserMoves(
    private val walking: Walking,
    private val poster: RoomPoster,
    private val wired: () -> WiredRooms?,
) {
    fun moveTo(
        firing: Firing,
        unit: RoomUnit,
        tile: Point,
        walkMode: Int = STOP,
    ): Boolean {
        val scope = firing.scope
        val through = firing.addons.filterIsInstance<MoveAddon>().firstNotNullOfOrNull { it.through(firing) }
        val landing = landing(scope, tile, through) ?: return false
        val origin = walking.stepOrigin(scope, unit)
        val from = origin?.tile ?: unit.tile
        val fromZ = origin?.z ?: unit.z
        val walkingNow = unit.isWalking
        // stepped onto the goal itself: nothing to walk back to
        val goal = unit.path.lastOrNull()?.takeUnless { origin != null && unit.path.size == 1 && unit.tile == it }
        val posture = unit.postureHeight
        val fromTop =
            scope.room.furni
                .at(unit.tile)
                .stepItem()
        val stepsBefore = unit.path.size
        val wiredWalk = walking.walksByWired(scope, unit)
        val stays = origin == null && !walkingNow && tile == unit.tile
        val body = unit.bodyDirection
        val head = unit.headDirection
        if (!stays) {
            walking.movedByWired(scope, unit)
            unit.dropStatus(UnitStatus.MOVE)
            walking.place(scope, unit, tile, landing.z)
        }
        val duration = MoveStyles.userDuration(firing)
        val entry =
            Movement.UnitMove(
                unit.index,
                Movement.Spot(from.x, from.y, FurniHeights.formatZ(fromZ + posture)),
                Movement.Spot(tile.x, tile.y, FurniHeights.formatZ(unit.z + posture)),
                if (walkingNow) Movement.UnitMove.WALK else Movement.UnitMove.SLIDE,
                duration,
                body,
                head,
            )
        if (firing.chain.depth > 0) firing.flash.movements.replace(entry) else firing.flash.movements.add(entry)
        val seated = unit.showsStatus(UnitStatus.SIT) || unit.showsStatus(UnitStatus.LAY)
        if (duration > 0 && !(walkingNow && seated)) poster.markWiredMove(scope, unit)
        // a bot's own stroll ends here; a walk from bot_move goes on
        if (goal != null &&
            (unit.kind !is UnitKind.Bot || wiredWalk) &&
            goesOn(scope, unit, walkMode, goal, stepsBefore)
        ) {
            walking.walkTo(scope, unit, goal, byWired = wiredWalk)
        }
        arrive(
            firing,
            unit,
            fromTop,
            scope.room.furni
                .at(tile)
                .stepItem(),
        )
        return true
    }

    /** Null when the unit may not land there; blocked tiles and units only pass with physics. */
    private fun landing(
        scope: RoomScope,
        tile: Point,
        through: MoveThrough?,
    ): Landing? {
        val room = scope.room
        if (!room.floor.contains(tile)) return null
        val landing =
            when (WalkTiles.at(room, tile).state) {
                TileState.Open, is TileState.Seat, is TileState.Bed, is TileState.Restricted -> {
                    Landing(null)
                }

                TileState.Void -> {
                    null
                }

                TileState.Blocked -> {
                    if (through != null && passes(room.furni.at(tile).furni, through)) {
                        RoomHeights.mapZ(room, tile)?.let(::Landing)
                    } else {
                        null
                    }
                }
            } ?: return null
        val passes = through?.unitIndexes.orEmpty()
        return landing.takeIf { room.units.all().none { unit -> unit.tile == tile && unit.index !in passes } }
    }

    private fun passes(
        furni: List<RoomFurni>,
        through: MoveThrough,
    ): Boolean = furni.filterNot { it.definition.canWalk }.all { it.id in through.furniIds }

    private class Landing(
        val z: Double?,
    )

    /** Mode 0: go on only when the goal is closer on foot from the target; a move user push also on a tie. */
    private fun goesOn(
        scope: RoomScope,
        unit: RoomUnit,
        walkMode: Int,
        goal: Point,
        stepsBefore: Int,
    ): Boolean =
        when (walkMode) {
            GO_ON -> {
                true
            }

            STOP -> {
                false
            }

            PUSH -> {
                walking.stepsTo(scope, unit, goal)?.let { it <= stepsBefore } == true
            }

            else -> {
                walking.stepsTo(scope, unit, goal)?.let { it < stepsBefore } == true
            }
        }

    private fun arrive(
        firing: Firing,
        unit: RoomUnit,
        fromTop: RoomFurni?,
        toTop: RoomFurni?,
    ) {
        if (fromTop === toTop) return
        if (firing.chain.depth > 0) {
            raise(firing.scope, unit, fromTop, toTop)
        } else {
            firing.flash.later { scope ->
                val engine = wired()
                if (engine == null) {
                    raise(scope, unit, fromTop, toTop)
                } else {
                    engine.resume(scope, firing.chain) { raise(scope, unit, fromTop, toTop) }
                }
            }
        }
    }

    private fun raise(
        scope: RoomScope,
        unit: RoomUnit,
        fromTop: RoomFurni?,
        toTop: RoomFurni?,
    ) {
        if (fromTop != null) {
            (fromTop.behaviour as? Steppable)?.onStepOff(scope, fromTop, unit)
            scope.raise(RoomEvent.StepOff(unit.index, fromTop.id))
        }
        if (toTop != null) {
            (toTop.behaviour as? Steppable)?.onStepOn(scope, toTop, unit)
            scope.raise(RoomEvent.StepOn(unit.index, toTop.id))
        }
    }

    companion object {
        const val KEEP_GOAL = 0
        const val GO_ON = 1
        const val STOP = 2
        const val PUSH = 3
    }
}
