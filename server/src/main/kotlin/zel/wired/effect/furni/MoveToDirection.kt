package zel.wired.effect.furni

import zel.room.Room
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.room.unit.UnitDirection
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets
import kotlin.random.Random

class MoveToDirection(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val moves: FurniMoves,
    private val random: Random,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    private val travel = HashMap<Int, Int>()

    override fun run(firing: Firing) {
        val start = settings.ints.getOrNull(START) ?: 0
        val usersBlock = (settings.ints.getOrNull(USERS) ?: 1) != 0
        Targets.furni(firing, this).filterNot { it.isWall }.forEach { furni ->
            val anchor = furni.anchor ?: return@forEach
            val current = travel[furni.id] ?: start
            // A user straight ahead is a collision, not a reason to turn: the furni stays, keeps its direction and
            // collides every firing.
            val ahead = if (usersBlock) unitAt(firing.scope.room, anchor.step(current)) else null
            if (ahead != null) {
                travel[furni.id] = current
                firing.scope.raise(RoomEvent.Collision(ahead.index, furni.id))
                return@forEach
            }
            // With the third int 0 a user ahead is a collision and blocks like a wall: the "when blocked" rule turns
            // the furni in the same firing.
            val hit = unitAt(firing.scope.room, anchor.step(current))
            hit?.let { firing.scope.raise(RoomEvent.Collision(it.index, furni.id)) }
            val direction = chosen(firing.scope.room, furni, anchor, current, unitsBlock = hit != null)
            travel[furni.id] = direction
            val tile = anchor.step(direction)
            // A user on the tile a turn chose only stops the furni: no collision.
            if (unitAt(firing.scope.room, tile) == null) moves.move(firing, furni, tile)
        }
    }

    private fun unitAt(
        room: Room,
        tile: Point,
    ) = room.units.all().firstOrNull { it.tile == tile }

    /** The "when blocked" rule turns for furni, walls and the map, and for users when [unitsBlock] (see [run]). */
    private fun chosen(
        room: Room,
        furni: RoomFurni,
        anchor: Point,
        current: Int,
        unitsBlock: Boolean,
    ): Int {
        val rule = settings.ints.getOrNull(BLOCKED) ?: 0
        var direction = current
        repeat(MAX_LOOKS) {
            if (moves.landing(room, furni, anchor.step(direction), unitsBlock = unitsBlock) != null) return direction
            if (rule == WAIT) return direction
            direction = turned(direction, rule)
        }
        return direction
    }

    private fun turned(
        direction: Int,
        rule: Int,
    ): Int =
        when (rule) {
            TURN_RIGHT_45 -> direction + 1
            TURN_RIGHT_90 -> direction + 2
            TURN_LEFT_45 -> direction - 1
            TURN_LEFT_90 -> direction - 2
            TURN_BACK -> direction + UnitDirection.COUNT / 2
            else -> random.nextInt(UnitDirection.COUNT)
        }.mod(UnitDirection.COUNT)

    companion object {
        const val MAX_RULE = 6
        private const val START = 0
        private const val BLOCKED = 1
        private const val USERS = 2
        private const val MAX_LOOKS = 8
        private const val WAIT = 0
        private const val TURN_RIGHT_45 = 1
        private const val TURN_RIGHT_90 = 2
        private const val TURN_LEFT_45 = 3
        private const val TURN_LEFT_90 = 4
        private const val TURN_BACK = 5
    }
}
