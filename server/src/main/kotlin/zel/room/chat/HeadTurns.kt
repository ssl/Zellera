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
) : RoomPart {
    private val rooms = ConcurrentHashMap<Room, MutableMap<RoomUnit, Long>>()

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
    ) {
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
    }
}
