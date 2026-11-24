package zel.wired.effect.user

import zel.game.effects.EffectRoles
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.Steppable
import zel.room.furni.behaviour.state.StateHooks
import zel.room.map.Point
import zel.room.tick.RoomTiming
import zel.room.tick.TickPart
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.UnitStatus
import zel.room.unit.path.TileState
import zel.room.unit.path.WalkTiles
import zel.room.unit.walk.Walking

/**
 * Wired teleports, stepped by the room's 0.5 s unit tick ([tick] runs before the units walk): the effect goes on at
 * once, the line comes 3 ticks later (fast: 1), the landing 1 tick after that, and the effect ends 3 ticks later
 * (fast: 1). Normal effect 4, fast effect 235. A walk asked between line and landing goes on from the landing tile.
 */
class Teleports(
    private val state: UserEffectState,
    private val walking: Walking,
    private val hooks: StateHooks,
    private val unfreezing: Unfreezing,
    private val host: UserEffectHost,
    private val landMillis: () -> Long = { LAND_MILLIS },
) {
    val tick: TickPart = TickPart(::tick)

    fun start(
        scope: RoomScope,
        unit: RoomUnit,
        target: RoomFurni,
        fast: Boolean = false,
    ) {
        if (target.anchor == null) return
        val effects = state.of(scope.room)
        val hop = TeleportTrip.Hop(target, effects.ticks + if (fast) FAST_LINE_TICKS else lineTicks())
        val trip = effects.teleports[unit.index]
        if (trip != null && trip.unit === unit) {
            trip.next = hop
            return
        }
        val effect = EffectRoles.of(if (fast) EffectRoles.TELEPORT_FAST else EffectRoles.TELEPORT)
        val worn = hooks.effect(unit)
        val held = effects.teleportEffects[unit.index]
        val before = if (held != null && worn == held.effect) held.before else worn
        val afterLanding = if (fast) FAST_EFFECT_AFTER_LANDING_TICKS else EFFECT_AFTER_LANDING_TICKS
        val ending = TeleportEffect(before, effect, afterLanding)
        effects.teleportEffects[unit.index] = ending
        effects.teleports[unit.index] = TeleportTrip(unit, hop, ending)
        if (worn != effect) hooks.giveEffect(scope, unit, effect, null)
        host.wake(scope, unit)
    }

    private fun tick(scope: RoomScope) {
        val room = scope.room
        val effects = state.of(room)
        val now = ++effects.ticks
        effects.teleports.values.toList().forEach { trip ->
            val current = effects.teleports[trip.unit.index] === trip && room.units[trip.unit.index] === trip.unit
            if (current) step(scope, trip, now)
        }
        val ending = effects.teleportEffects.filterValues { it.offTick == now }
        ending.forEach { (index, it) ->
            effects.teleportEffects.remove(index)
            room.units[index]?.let { unit -> effectBack(scope, unit, it) }
        }
    }

    private fun step(
        scope: RoomScope,
        trip: TeleportTrip,
        now: Long,
    ) {
        val landsAt = trip.landsAt
        if (landsAt == null) {
            if (now >= trip.hop.lineTick) line(scope, trip, now)
        } else if (now >= landsAt) {
            land(scope, trip, now)
        }
    }

    private fun line(
        scope: RoomScope,
        trip: TeleportTrip,
        now: Long,
    ) {
        walking.stopWithEmptySlot(scope, trip.unit)
        trip.landsAt = now + 1
        // The furni walked off and onto fire with the line, a tick before the landing.
        val anchor = trip.hop.target.anchor ?: return
        val room = scope.room
        // A bot lands on the target tile even when furni block it.
        val to = if (trip.unit.kind is UnitKind.Bot) anchor else landing(scope, anchor, trip.hop.target.direction)
        trip.landing = to
        arrive(scope, trip.unit, room.furni.at(trip.unit.tile).stepItem(), room.furni.at(to).stepItem())
    }

    private fun land(
        scope: RoomScope,
        trip: TeleportTrip,
        now: Long,
    ) {
        val unit = trip.unit
        val effects = state.of(scope.room)
        trip.landsAt = null
        val goal = if (unit.kind is UnitKind.Player) unit.path.lastOrNull() else null
        trip.landing?.let { place(scope, unit, it) }
        trip.landing = null
        if (trip.ending.offTick == null) trip.ending.offTick = now + trip.ending.afterLanding
        if (effects.frozen[unit.index]?.endsOnTeleport == true) unfreezing.unfreeze(scope, unit)
        if (effects.teleports[unit.index] !== trip) return
        val next = trip.next
        if (next == null) {
            effects.teleports.remove(unit.index)
            if (goal != null && goal != unit.tile && !unit.isWalking) walking.resumeAfterTeleport(scope, unit, goal)
            return
        }
        trip.next = null
        trip.hop = next
        if (now >= next.lineTick) line(scope, trip, now)
    }

    private fun place(
        scope: RoomScope,
        unit: RoomUnit,
        to: Point,
    ) {
        val horse = walking.horseOf(scope, unit)
        unit.dropStatus(UnitStatus.MOVE)
        if (horse != null) walking.place(scope, horse, to)
        walking.place(scope, unit, to)
    }

    /** A blocked target tile: the furni's facing tile first, then its other neighbours clockwise. Units never block. */
    private fun landing(
        scope: RoomScope,
        target: Point,
        facing: Int,
    ): Point {
        if (standable(scope, target)) return target
        return (0 until 8).map { target.step(facing + it) }.firstOrNull { standable(scope, it) } ?: target
    }

    private fun standable(
        scope: RoomScope,
        tile: Point,
    ): Boolean =
        when (WalkTiles.at(scope.room, tile).state) {
            TileState.Open, is TileState.Seat, is TileState.Bed, is TileState.Restricted -> true
            TileState.Void, TileState.Blocked -> false
        }

    private fun arrive(
        scope: RoomScope,
        unit: RoomUnit,
        fromTop: RoomFurni?,
        toTop: RoomFurni?,
    ) {
        if (fromTop != null && fromTop !== toTop) {
            (fromTop.behaviour as? Steppable)?.onStepOff(scope, fromTop, unit)
            scope.raise(RoomEvent.StepOff(unit.index, fromTop.id))
        }
        if (toTop != null && toTop !== fromTop) {
            (toTop.behaviour as? Steppable)?.onStepOn(scope, toTop, unit)
            scope.raise(RoomEvent.StepOn(unit.index, toTop.id))
            if (unit.kind is UnitKind.Bot) scope.raise(RoomEvent.BotReachedFurni(unit.index, toTop.id))
        }
    }

    private fun effectBack(
        scope: RoomScope,
        unit: RoomUnit,
        ending: TeleportEffect,
    ) {
        if (hooks.effect(unit) == ending.effect) hooks.giveEffect(scope, unit, ending.before, null)
    }

    /** The normal line's tick: one before the landing, which [landMillis] sets (at least the fast timing). */
    private fun lineTicks(): Long {
        val landing = (landMillis() + RoomTiming.TICK_MILLIS - 1) / RoomTiming.TICK_MILLIS
        return landing.coerceAtLeast(FAST_LINE_TICKS + 1) - 1
    }

    companion object {
        /** Normal landing on the 4th tick after the effect (2.0 s if asked in a tick); wired.teleport.duration.ms. */
        const val LAND_MILLIS = 2000L
        const val EFFECT_AFTER_LANDING_TICKS = 3
        const val FAST_LINE_TICKS = 1L
        const val FAST_EFFECT_AFTER_LANDING_TICKS = 1
    }
}
