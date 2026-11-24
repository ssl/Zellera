package zel.room.furni.behaviour.state

import zel.game.effects.EffectRoles
import zel.protocol.generated.NotificationDialogOut
import zel.protocol.generated.NotificationDialogOutCodec
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.Usable
import zel.room.map.Point
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection
import zel.room.unit.UnitKind
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.roundToInt

class Cannon(
    private val part: StatePart,
) : Usable {
    /** The shot of one cannon kind: directions relative to the cannon's own rotation. */
    data class Shot(
        val direction: Int = PIRATE_DIRECTION,
        val range: Int = PIRATE_RANGE,
        val width: Int = PIRATE_WIDTH,
        val offset: Int = PIRATE_OFFSET,
        val fireDelayMillis: Long = PIRATE_FIRE_DELAY,
        val loadDelayMillis: Long = PIRATE_LOAD_DELAY,
        val noUsageDirections: Set<Int> = PIRATE_NO_USAGE,
    ) {
        fun lineOfFire(
            tiles: List<Point>,
            rotation: Int,
        ): List<Point> {
            val own = tiles.toSet()
            val aim = Math.floorMod(rotation + direction, UnitDirection.COUNT)
            val front = tiles.filter { it.step(aim) !in own }
            val sideways = (width - 1) / 2
            return front
                .flatMap { tile ->
                    (-sideways..sideways).flatMap { side ->
                        val start = tile.takeSteps(side, aim + 2)
                        (offset until offset + range).map { start.takeSteps(it, aim) }
                    }
                }.distinct()
                .filter { it !in own }
        }

        fun firedFrom(
            tiles: List<Point>,
            rotation: Int,
            tile: Point,
        ): Boolean {
            if (tiles.isEmpty() || tiles.minOf { max(abs(it.x - tile.x), abs(it.y - tile.y)) } != 1) return false
            val relative = Math.floorMod(directionFromCentre(tiles, tile) - rotation, UnitDirection.COUNT)
            return relative !in noUsageDirections
        }

        private fun Point.takeSteps(
            count: Int,
            direction: Int,
        ): Point {
            var at = this
            repeat(abs(count)) { at = at.step(if (count < 0) direction + HALF_TURN else direction) }
            return at
        }

        companion object {
            fun of(params: String): Shot {
                val values =
                    params
                        .split(';')
                        .map { pair -> pair.split('=', limit = 2) }
                        .filter { it.size == 2 }
                        .associate { it[0].trim() to it[1].trim() }
                val number = { key: String, fallback: Int -> values[key]?.toIntOrNull() ?: fallback }
                return Shot(
                    direction = number("dir", PIRATE_DIRECTION),
                    range = number("range", PIRATE_RANGE),
                    width = number("width", PIRATE_WIDTH),
                    offset = number("offset", PIRATE_OFFSET),
                    fireDelayMillis = number("fire_delay", PIRATE_FIRE_DELAY.toInt()).toLong(),
                    loadDelayMillis = number("load_delay", PIRATE_LOAD_DELAY.toInt()).toLong(),
                    noUsageDirections =
                        values["no_usage_dirs"]
                            ?.split(',')
                            ?.mapNotNull { it.trim().toIntOrNull() }
                            ?.toSet() ?: PIRATE_NO_USAGE,
                )
            }

            /** The compass direction (0 north, clockwise) from the centre of [tiles] to [to], to the nearest eighth. */
            fun directionFromCentre(
                tiles: List<Point>,
                to: Point,
            ): Int {
                val dx = to.x - tiles.map { it.x }.average()
                val dy = to.y - tiles.map { it.y }.average()
                val eighths = Math.toDegrees(atan2(dx, -dy)) / DEGREES_PER_DIRECTION
                return Math.floorMod(eighths.roundToInt(), UnitDirection.COUNT)
            }
        }
    }

    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        val states = part.of(scope.room)
        if ((states.coolUntil[furni.id] ?: 0L) > scope.now()) return
        val shot = Shot.of(furni.definition.params)
        val firer = scope.room.units.ofPlayer(playerId)
        val tiles = furni.tiles()
        if (firer != null && !shot.firedFrom(tiles, furni.direction, firer.tile)) return
        states.coolUntil[furni.id] = scope.now() + shot.loadDelayMillis
        StateShow.data(scope, furni, if (furni.state == FIRED) LOADED else FIRED)
        scope.raise(RoomEvent.StateChanged(furni.id, firer?.index))
        val zone = shot.lineOfFire(tiles, furni.direction).toSet()
        part.later(scope, furni, shot.fireDelayMillis) { later, _ ->
            val hit =
                later.room.units
                    .all()
                    .filter { it.tile in zone && it.kind is UnitKind.Player && !spared(later, it) }
            hit.forEach { part.hooks.giveEffect(later, it, EffectRoles.of(EffectRoles.CANNON), null) }
            part.later(later, furni, KICK_AFTER_HIT_MILLIS) { kicking, _ ->
                val present = kicking.room.units.all()
                hit.filter { it in present }.forEach { kick(kicking, it) }
            }
        }
    }

    private fun spared(
        scope: RoomScope,
        unit: RoomUnit,
    ): Boolean {
        val playerId = (unit.kind as UnitKind.Player).playerId
        return playerId == scope.room.info.ownerId || part.hooks.unkickable(playerId)
    }

    private fun kick(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        val playerId = (unit.kind as UnitKind.Player).playerId
        scope.out.send(
            Lane.Now,
            Audience.Player(playerId),
            NotificationDialogOutCodec,
            NotificationDialogOut(KICKED_DIALOG, emptyList()),
        )
        part.hooks.sendOut(scope, unit)
    }

    private companion object {
        const val LOADED = "0"
        const val FIRED = "1"
        const val HALF_TURN = 4
        const val DEGREES_PER_DIRECTION = 45.0
        const val KICK_AFTER_HIT_MILLIS = 1500L
        const val KICKED_DIALOG = "room.kick.cannonball"

        const val PIRATE_DIRECTION = 6
        const val PIRATE_RANGE = 4
        const val PIRATE_WIDTH = 3
        const val PIRATE_OFFSET = 1
        const val PIRATE_FIRE_DELAY = 750L
        const val PIRATE_LOAD_DELAY = 2000L
        val PIRATE_NO_USAGE = setOf(5, 6, 7)
    }
}
