package zel.room.furni.roller

import zel.protocol.generated.SlideObjectBundleOutCodec
import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.Steppable
import zel.room.furni.place.Interactions
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.room.unit.UnitStatus
import zel.room.unit.walk.Walking
import java.util.concurrent.ConcurrentHashMap

/** Every member runs under the room's lock. */
class Rollers(
    maxUnitsPerRoller: Int = 1,
    private val roomSpeed: Int = DEFAULT_SPEED,
    private val heightTolerance: Double = DEFAULT_HEIGHT_TOLERANCE,
) : RoomPart {
    private val rooms = ConcurrentHashMap<Room, RollerRoom>()
    private val units = RollingUnits(maxUnitsPerRoller)

    override fun load(room: Room) {
        rooms[room] = RollerRoom()
    }

    override fun unload(scope: RoomScope) {
        rooms.remove(scope.room)
    }

    fun afterSteps(
        scope: RoomScope,
        walking: Walking,
    ) {
        val room = scope.room
        val state = rooms.getOrPut(room) { RollerRoom() }
        state.ticks++
        state.prune(room.units)
        fireOwed(scope, state)
        val speed = speedOf(room)
        if (state.speed != null && state.speed != speed) state.sinceRoll = 0
        state.speed = speed
        if (state.sinceRoll < Int.MAX_VALUE) state.sinceRoll++
        if (state.sinceRoll <= speed) return
        state.sinceRoll = 0
        val rollers = room.furni.withInteraction(Interactions.ROLLER)
        room.furni.withInteraction(QueueSpeed.INTERACTION).forEach { QueueSpeed.animate(scope, it) }
        if (rollers.isEmpty()) return
        val pass = RollerPass(scope, walking, state)
        rollers.forEach { roller ->
            val target = RollerTarget.of(room, roller, heightTolerance)
            val unitSlides = units.roll(pass, roller, target)
            val items = FurniRolls.roll(pass, roller, target)
            val from = roller.anchor ?: return@forEach
            RollerSlides.bundles(from, target.tile, roller.id, unitSlides, items).forEach {
                scope.out.send(Lane.Now, Audience.Room, SlideObjectBundleOutCodec, it)
            }
        }
    }

    private fun fireOwed(
        scope: RoomScope,
        state: RollerRoom,
    ) {
        val due = state.owed.filter { it.dueTick <= state.ticks }
        if (due.isEmpty()) return
        state.owed.removeAll(due.toSet())
        due.forEach { step ->
            val unit = scope.room.units[step.unitIndex] ?: return@forEach
            if (unit.isWalking || unit.showsStatus(UnitStatus.MOVE) || unit.tile != step.tile) return@forEach
            // The arrival sends the unit's line at the new tile with this tick (the slide alone sent none).
            unit.changed = true
            val left = step.leftTopId?.let { scope.room.furni[it] }
            val entered = step.newTopId?.let { scope.room.furni[it] }
            if (left != null && left !== entered) stepOff(scope, left, unit)
            if (entered != null) stepOn(scope, entered, unit)
        }
    }

    private fun stepOff(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        (furni.behaviour as? Steppable)?.onStepOff(scope, furni, unit)
        scope.raise(RoomEvent.StepOff(unit.index, furni.id))
    }

    private fun stepOn(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        (furni.behaviour as? Steppable)?.onStepOn(scope, furni, unit)
        scope.raise(RoomEvent.StepOn(unit.index, furni.id))
    }

    companion object {
        const val MAX_UNITS_KEY = "rollers.units_per_roller"

        const val DEFAULT_SPEED = 3

        const val DEFAULT_HEIGHT_TOLERANCE = 0.5
    }

    private fun speedOf(room: Room): Int =
        room.furni
            .withInteraction(QueueSpeed.INTERACTION)
            .firstOrNull()
            ?.let { QueueSpeed.speedOf(it.state) }
            ?: roomSpeed
}
