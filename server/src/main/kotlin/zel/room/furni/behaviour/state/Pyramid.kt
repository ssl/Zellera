package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Ticking
import zel.room.unit.RoomUnit
import zel.util.MILLIS_PER_SECOND

/** Rises and sinks by itself; each position lasts a random time within [pauseSeconds]. */
class Pyramid(
    part: StatePart,
    private val pauseSeconds: () -> IntRange = { DEFAULT_PAUSE },
) : Multistate(part, toggles = false),
    Ticking {
    override fun mayUse(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
        playerId: Int,
    ): Boolean = false

    override fun onTick(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        val due = part.of(scope.room).pyramidDue
        val now = scope.now()
        if (now < due.getOrPut(furni.id) { now + pauseMillis() }) return
        due[furni.id] = now + pauseMillis()
        // A pyramid somebody stands on skips this turn and waits a new pause.
        if (occupied(scope, furni)) return
        StateShow.data(scope, furni, if (furni.state == RAISED) LOWERED else RAISED)
    }

    private fun occupied(
        scope: RoomScope,
        furni: RoomFurni,
    ): Boolean {
        val tiles = furni.tiles().toSet()
        return scope.room.units
            .all()
            .any { it.tile in tiles }
    }

    private fun pauseMillis(): Long {
        val pause = pauseSeconds()
        val shortest = maxOf(1, pause.first)
        val longest = maxOf(shortest, pause.last)
        return (shortest + part.random.nextInt(longest - shortest + 1)) * MILLIS_PER_SECOND
    }

    companion object {
        private const val RAISED = "1"
        private const val LOWERED = "0"
        val DEFAULT_PAUSE = 1..18
    }
}
