package zel.room.chat

import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.map.Point
import zel.room.tick.TickPart
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection
import zel.room.unit.UnitKind
import zel.room.unit.UnitStatus
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class HeadTurns(
    private val asleep: (RoomUnit) -> Boolean,
    private val show: (RoomScope, RoomUnit) -> Unit = { _, _ -> },
    private val later: (Int, Long, (RoomScope) -> Unit) -> Unit = { _, _, _ -> },
) : RoomPart {
    private val rooms = ConcurrentHashMap<Room, MutableMap<RoomUnit, Long>>()
    private val turning: MutableSet<RoomUnit> = ConcurrentHashMap.newKeySet()

    val tick: TickPart =
        TickPart { scope ->
            val turned = rooms[scope.room] ?: return@TickPart
            val due = turned.filterValues { it <= scope.now() }.keys
            due.forEach { unit ->
                turned.remove(unit)
                if (scope.room.units[unit.index] === unit && unit.headDirection != unit.bodyDirection) {
                    unit.headDirection = unit.bodyDirection
                    unit.changed = true
                }
            }
        }

    override fun load(room: Room) {
        rooms[room] = LinkedHashMap()
    }

    override fun unload(scope: RoomScope) {
        rooms.remove(scope.room)
    }

    fun heard(
        scope: RoomScope,
        speaker: RoomUnit,
        text: String = "",
    ) {
        if (fullTurn(text)) turnAround(scope, speaker)
        val turned = rooms.getOrPut(scope.room) { LinkedHashMap() }
        scope.room.units
            .all()
            .filter { it !== speaker && mayTurn(it) }
            .forEach { listener ->
                val head = headToward(listener.bodyDirection, listener.tile, speaker.tile) ?: return@forEach
                if (listener.headDirection != head) {
                    listener.headDirection = head
                    listener.changed = true
                }
                turned[listener] = scope.now() + TURN_MILLIS
            }
    }

    private fun fullTurn(text: String): Boolean = text.lowercase().split(WORD_GAP).any { it.hashCode() == SPIN_WORD }

    fun lift(
        scope: RoomScope,
        unit: RoomUnit,
    ) = move(scope, unit, LIFT)

    private fun turnAround(
        scope: RoomScope,
        unit: RoomUnit,
    ) = move(scope, unit, FULL)

    private fun move(
        scope: RoomScope,
        unit: RoomUnit,
        motion: Motion,
    ) {
        if (unit.kind !is UnitKind.Player || unit.isWalking || !turning.add(unit)) return
        val from = Turn(unit.tile, unit.bodyDirection, unit.z)
        for (step in 1..motion.steps) {
            later(scope.room.info.id, step * motion.stepMillis) { later -> step(later, unit, from, step, motion) }
        }
    }

    private fun step(
        scope: RoomScope,
        unit: RoomUnit,
        from: Turn,
        step: Int,
        motion: Motion,
    ) {
        val last = step == motion.steps
        if (last) turning.remove(unit)
        if (scope.room.units[unit.index] !== unit || unit.tile != from.tile || unit.isWalking) return
        if (motion.turns) {
            unit.bodyDirection = Math.floorMod(from.body + step, UnitDirection.COUNT)
            unit.headDirection = unit.bodyDirection
        }
        val rise = if (last) 0.0 else motion.rise * (1 - abs(2.0 * step / motion.steps - 1))
        unit.z = from.z + rise
        show(scope, unit)
        unit.z = from.z
    }

    private class Motion(
        val steps: Int,
        val stepMillis: Long,
        val rise: Double,
        val turns: Boolean,
    )

    private class Turn(
        val tile: Point,
        val body: Int,
        val z: Double,
    )

    // users and bots turn, pets do not; a sitting bot keeps its head
    private fun mayTurn(unit: RoomUnit): Boolean =
        when (unit.kind) {
            is UnitKind.Player -> true
            is UnitKind.Bot -> !unit.showsStatus(UnitStatus.SIT)
            is UnitKind.Pet -> false
        } &&
            !unit.isWalking &&
            !unit.showsStatus(UnitStatus.LAY) &&
            !unit.showsStatus(UnitStatus.SLEEP) &&
            !asleep(unit)

    /**
     * One step off the body faces the speaker; two steps off turn one step toward him; three steps off turn one step
     * only while the speaker is less than 135 degrees off the body (more to the side than behind), else no turn;
     * behind: no turn.
     */
    private fun headToward(
        body: Int,
        listener: Point,
        speaker: Point,
    ): Int? {
        val direction = UnitDirection.between(listener, speaker) ?: return null
        val apart = Math.floorMod(direction - body, UnitDirection.COUNT)
        val toward = if (apart < UnitDirection.COUNT / 2) 1 else -1
        return when {
            apart <= 1 || apart == UnitDirection.COUNT - 1 -> {
                direction
            }

            apart == UnitDirection.COUNT / 2 -> {
                null
            }

            apart == 2 || apart == UnitDirection.COUNT - 2 -> {
                Math.floorMod(body + toward, UnitDirection.COUNT)
            }

            moreSideThanBehind(body, speaker.x - listener.x, speaker.y - listener.y) -> {
                Math.floorMod(body + toward, UnitDirection.COUNT)
            }

            else -> {
                null
            }
        }
    }

    // a diagonal body has a three-steps-off speaker on an axis, exactly 135 degrees off: never
    private fun moreSideThanBehind(
        body: Int,
        dx: Int,
        dy: Int,
    ): Boolean {
        if (UnitDirection.isDiagonal(body)) return false
        val facing = Point(0, 0).step(body)
        val behind = -(dx * facing.x + dy * facing.y)
        val side = abs(dx * facing.y - dy * facing.x)
        return behind < side
    }

    companion object {
        /** The head turns back 5.0 s after the last line heard. */
        const val TURN_MILLIS = 5_000L

        private const val SPIN_WORD = -399097975
        private val FULL = Motion(8, 150L, 0.5, turns = true)
        private val LIFT = Motion(4, 110L, 0.4, turns = false)
        private val WORD_GAP = Regex("[^\\p{L}]+")
    }
}
