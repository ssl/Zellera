package zel.wired.variable

import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.wired.Hundredths
import zel.wired.addon.Projectiles
import zel.wired.effect.furni.FloorMove
import zel.wired.timing.RoomPoster
import kotlin.math.floor
import kotlin.math.roundToLong

/** `@projectile.animation.*`: one tile per step from its start; travelling ends as the last step starts. */
class ProjectileFlights(
    private val spaces: (Room) -> VariableSpace,
    private val poster: RoomPoster,
    private val raise: (VariableOn, VariableChange) -> Unit,
) : Projectiles {
    override fun fly(
        scope: RoomScope,
        move: FloorMove,
        distance: Double,
        steps: List<Double>,
    ) {
        if (steps.isEmpty() || distance <= 0.0) return
        val feet =
            scope.room.units
                .all()
                .associate { it.index to (it.tile to it.z) }
        val flight = Flight(move, distance, steps.size, feet)
        spaces(scope.room).flights[move.furni.id] = flight
        var start = 0.0
        steps.forEachIndexed { index, millis ->
            val at = start.roundToLong()
            poster.post(scope.room.info.id, at) { step(it, flight, index + 1) }
            start += millis
        }
    }

    private fun step(
        scope: RoomScope,
        flight: Flight,
        step: Int,
    ) {
        val flights = spaces(scope.room).flights
        val furni = flight.move.furni
        if (flights[furni.id] !== flight) return
        if (scope.room.furni[furni.id] !== furni) {
            flights.remove(furni.id)
            return
        }
        val entered = flight.reach(step)
        val users =
            if (entered) {
                scope.room.units
                    .all()
                    .count { flight.hits(it) }
            } else {
                0
            }
        val others =
            if (entered) {
                scope.room.furni
                    .at(flight.at)
                    .furni
                    .count { it !== furni && flight.hits(it) }
            } else {
                0
            }
        val on = VariableOn(scope)
        val holder = Holder.of(furni)
        if (users > 0) {
            val old = flight.users
            flight.users += users
            counted(on, holder, USER_COLLISIONS, old, flight.users)
        }
        if (others > 0) {
            val old = flight.furni
            flight.furni += others
            counted(on, holder, FURNI_COLLISIONS, old, flight.furni)
        }
        if (step == flight.steps) {
            flight.travelling = false
            raise(on, VariableChange(IS_TRAVELLING, 0, holder, ChangeOrigin.IN_ROOM, ChangeType.DELETED, null, null))
        }
    }

    private fun counted(
        on: VariableOn,
        holder: Holder,
        id: String,
        old: Int,
        new: Int,
    ) {
        val type = ChangeType.VALUE_CHANGED
        raise(on, VariableChange(id, 0, holder, ChangeOrigin.IN_ROOM, type, old.toLong(), new.toLong()))
    }

    class Flight(
        val move: FloorMove,
        private val distance: Double,
        val steps: Int,
        private val feet: Map<Int, Pair<Point, Double>> = emptyMap(),
    ) {
        var tiles = 0
        var users = 0
        var furni = 0
        var at: Point = move.from
        var z: Double = move.fromZ
        var travelling = true

        fun reach(step: Int): Boolean {
            val part = minOf(step.toDouble(), distance) / distance
            val before = at
            at = Point(round(move.from.x, move.to.x, part), round(move.from.y, move.to.y, part))
            z = move.fromZ + (move.toZ - move.fromZ) * part
            tiles = step
            return at != before
        }

        /** A user the landing put on top of the projectile counts at the height he stood before. */
        fun hits(unit: RoomUnit): Boolean {
            if (unit.tile != at) return false
            val before = feet[unit.index]?.takeIf { it.first == unit.tile }?.second ?: unit.z
            val base = minOf(unit.z, before)
            return z >= base && z <= base + USER_HEIGHT
        }

        fun hits(other: RoomFurni): Boolean = z >= other.z && z < other.top

        fun values(): Map<String, Long> =
            buildMap {
                put(TILES_TRAVELLED, tiles.toLong())
                put(USER_COLLISIONS, users.toLong())
                put(FURNI_COLLISIONS, furni.toLong())
                put(POSITION_X, at.x.toLong())
                put(POSITION_Y, at.y.toLong())
                put(POSITION_ALTITUDE, Hundredths.of(z))
                if (travelling) put(IS_TRAVELLING, 0)
            }

        private fun round(
            from: Int,
            to: Int,
            part: Double,
        ): Int = floor(from + (to - from) * part + HALF).toInt()
    }

    companion object {
        const val TILES_TRAVELLED = "-2100"
        const val USER_COLLISIONS = "-2101"
        const val FURNI_COLLISIONS = "-2102"
        const val POSITION_X = "-2103"
        const val POSITION_Y = "-2104"
        const val POSITION_ALTITUDE = "-2105"
        const val IS_TRAVELLING = "-2106"

        const val USER_HEIGHT = 2.0

        private const val HALF = 0.5
    }
}
