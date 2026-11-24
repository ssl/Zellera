package zel.wired.addon

import zel.room.map.Point
import zel.room.unit.UnitDirection
import zel.wired.box.BoxSettings
import zel.wired.effect.furni.FloorMove
import zel.wired.effect.furni.MoveAddon
import zel.wired.effect.furni.MoveShape
import zel.wired.effect.furni.MoveStyle
import zel.wired.firing.Firing
import zel.wired.firing.Movement
import zel.wired.firing.Targets
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class MoveRotateToDirection(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val projectiles: Projectiles = Projectiles.None,
) : MoveAddon {
    private val system = settings.intAt(SYSTEM, STRAIGHT)
    private val turnsFurni = settings.intAt(TURN_FURNI, 1) != 0
    private val offset = Math.floorMod(settings.intAt(OFFSET), UnitDirection.COUNT)
    private val flies = settings.intAt(FLIGHT) != 0
    private val turnShooter = settings.intAt(TURN_SHOOTER) != 0

    private var turnedIn: Firing? = null

    override fun style(
        firing: Firing,
        style: MoveStyle,
    ): MoveStyle = if (turnsFurni) style.copy(turnsSlide = true) else style

    override fun shape(
        firing: Firing,
        shape: MoveShape,
        from: Point,
        to: Point,
    ): MoveShape {
        val dx = to.x - from.x
        val dy = to.y - from.y
        if (dx == 0 && dy == 0) return shape
        return MoveShape(
            if (turnsFurni) (direction(dx, dy) + offset) % UnitDirection.COUNT else shape.direction,
            if (flies) flightMillis(dx, dy) else shape.duration,
        )
    }

    override fun landed(
        firing: Firing,
        move: FloorMove,
    ) {
        val dx = move.to.x - move.from.x
        val dy = move.to.y - move.from.y
        if (dx == 0 && dy == 0) return
        if (flies) projectiles.fly(firing.scope, move, distance(dx, dy), steps(dx, dy))
        if (!turnShooter || turnedIn === firing) return
        val shooter = Targets.units(firing, this, SHOOTER_SLOT).firstOrNull() ?: return
        turnedIn = firing
        val direction = direction(dx, dy)
        shooter.bodyDirection = direction
        shooter.headDirection = direction
        firing.flash.movements.add(Movement.UnitTurn(shooter.index, direction))
    }

    private fun flightMillis(
        dx: Int,
        dy: Int,
    ): Int =
        steps(dx, dy)
            .sum()
            .roundToLong()
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

    private fun distance(
        dx: Int,
        dy: Int,
    ): Double {
        val alongX = if (settings.intAt(COUNT_X, 1) != 0) abs(dx) else 0
        val alongY = if (settings.intAt(COUNT_Y, 1) != 0) abs(dy) else 0
        return hypot(alongX.toDouble(), alongY.toDouble())
    }

    private fun steps(
        dx: Int,
        dy: Int,
    ): List<Double> {
        val distance = distance(dx, dy)
        val tiles = distance.toInt()
        val first = settings.intAt(FIRST_TILE_MILLIS, FRESH_FIRST_TILE).toLong()
        val less = settings.intAt(LESS_PER_TILE, FRESH_LESS).toLong()
        val whole = (0 until tiles).map { maxOf(1L, first - less * it).toDouble() }
        val rest = distance - tiles
        return if (rest > 0.0) whole + rest * maxOf(1L, first - less * tiles) else whole
    }

    private fun direction(
        dx: Int,
        dy: Int,
    ): Int {
        val eighths = Math.floorMod((atan2(dx.toDouble(), -dy.toDouble()) / (PI / 4)).roundToInt(), UnitDirection.COUNT)
        val fourWay = system == PREFER_VERTICAL || system == PREFER_HORIZONTAL
        if (!fourWay || eighths % 2 == 0) return eighths
        val vertical = system == PREFER_VERTICAL
        return listOf(
            eighths - 1,
            eighths + 1,
        ).map { Math.floorMod(it, UnitDirection.COUNT) }.first { (it % QUARTER == 0) == vertical }
    }

    private companion object {
        const val QUARTER = 4
        const val TURN_FURNI = 0
        const val SYSTEM = 1
        const val FLIGHT = 2
        const val FIRST_TILE_MILLIS = 4
        const val COUNT_X = 6
        const val COUNT_Y = 7
        const val LESS_PER_TILE = 9
        const val OFFSET = 10
        const val TURN_SHOOTER = 12
        const val FRESH_FIRST_TILE = 100
        const val FRESH_LESS = 4
        const val STRAIGHT = 0
        const val PREFER_VERTICAL = 2
        const val PREFER_HORIZONTAL = 3
        const val SHOOTER_SLOT = 2
    }
}
