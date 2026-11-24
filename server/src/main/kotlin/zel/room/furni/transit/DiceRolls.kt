package zel.room.furni.transit

import zel.protocol.generated.DiceValueOut
import zel.protocol.generated.DiceValueOutCodec
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

/** Under the room lock. */
class DiceRolls(
    private val random: Random,
) {
    fun roll(
        scope: RoomScope,
        state: RoomTransit,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        if (!mayTouch(furni, unit) || furni.state == ROLLING || furni.id in state.rolls) return
        show(scope, furni, ROLLING, ROLLING.toInt())
        state.rolls[furni.id] = DiceRoll(unit.index, scope.now() + (named(furni, "delay") ?: ROLL_MILLIS))
    }

    fun close(
        scope: RoomScope,
        state: RoomTransit,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        if (!mayTouch(furni, unit) || furni.id in state.rolls || furni.state == Rides.CLOSED) return
        show(scope, furni, Rides.CLOSED, OFF)
        furni.dirty = true
    }

    fun tick(
        scope: RoomScope,
        state: RoomTransit,
    ) {
        state.rolls.entries.toList().forEach { (id, roll) ->
            if (scope.now() + Rides.SLACK_MILLIS < roll.due) return@forEach
            state.rolls.remove(id)
            val furni = scope.room.furni[id] ?: return@forEach
            val faces = max(1, named(furni, "states")?.toInt() ?: furni.definition.stateCount)
            val face = random.nextInt(faces) + 1
            show(scope, furni, face.toString(), face)
            furni.dirty = true
            // the face fires the dice's wf_trg_stuff_state stack in the same batch
            scope.raise(RoomEvent.StateChanged(furni.id, null, settled = true))
        }
    }

    /** The stored state and the value the room sees differ only for a dice switched off. */
    private fun show(
        scope: RoomScope,
        furni: RoomFurni,
        state: String,
        shown: Int,
    ) {
        furni.state = state
        scope.out.send(Lane.Now, Audience.Room, DiceValueOutCodec, DiceValueOut(furni.id, shown))
    }

    /** A value of the definition's params, written `states=8,delay=2000`. */
    private fun named(
        furni: RoomFurni,
        key: String,
    ): Long? =
        furni.definition.params
            .split(',')
            .map { it.split('=', limit = 2) }
            .firstOrNull { it.size == 2 && it[0].trim() == key }
            ?.let { it[1].trim().toLongOrNull() }
            ?.takeIf { it > 0 }

    private fun mayTouch(
        furni: RoomFurni,
        unit: RoomUnit,
    ): Boolean {
        val anchor = furni.anchor ?: return false
        val near = max(abs(unit.tile.x - anchor.x), abs(unit.tile.y - anchor.y)) == 1
        return unit.kind is UnitKind.Player && near
    }

    companion object {
        const val ROLLING = "-1"

        /** From the -1 to the face. */
        const val ROLL_MILLIS = 2_250L

        /** The shown value of a dice switched off. */
        const val OFF = 100
    }
}
