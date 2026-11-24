package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.DrawnFront
import zel.room.furni.RoomFurni
import zel.room.furni.Usable
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection
import zel.room.unit.UnitStatus
import zel.room.unit.path.TileState
import zel.room.unit.path.WalkTiles
import kotlin.math.abs

/**
 * A vending machine serves only on the tile in front of its picture, the side its art is drawn facing
 * ([DrawnFront]). A customer on that tile is served there; one anywhere else is walked to it and served on arrival;
 * when that tile cannot be walked to, nothing happens. A machine marked [anySide] also serves a customer standing on
 * any tile next to it.
 */
class VendingMachine(
    private val part: StatePart,
    private val effects: Boolean,
    private val anySide: Boolean = false,
) : Usable {
    private val arrival =
        ArrivalWatch(
            { scope, furni, millis, run -> part.later(scope, furni, millis, run) },
            WATCH_TICKS,
            mustStop = true,
        ) { scope, machine, unit -> if (serves(machine, unit)) vend(scope, machine, unit) }

    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        val unit = scope.room.units.ofPlayer(playerId) ?: return
        val front = DrawnFront.tile(furni) ?: return
        if (serves(furni, unit)) {
            vend(scope, furni, unit)
            return
        }
        val open = WalkTiles.at(scope.room, front).state.let { it == TileState.Open || it is TileState.Seat }
        if (open && part.hooks.walkTo(scope, unit, front)) arrival.watch(scope, furni, unit, front)
    }

    private fun serves(
        furni: RoomFurni,
        unit: RoomUnit,
    ): Boolean = unit.tile == DrawnFront.tile(furni) || (anySide && isBeside(furni, unit))

    private fun isBeside(
        furni: RoomFurni,
        unit: RoomUnit,
    ): Boolean {
        val at = unit.tile
        val tiles = furni.tiles()
        return at !in tiles && tiles.any { abs(it.x - at.x) <= 1 && abs(it.y - at.y) <= 1 }
    }

    private fun vend(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        if (furni.state != BUSY) {
            StateShow.data(scope, furni, BUSY)
            scope.raise(RoomEvent.StateChanged(furni.id, unit.index))
        }
        faceMachine(furni, unit)
        part.later(scope, furni, HAND_ITEM_MILLIS) { inner, target ->
            if (inner.room.units[unit.index] !== unit) return@later
            // a machine that holds nothing leaves the customer's hand item as it is
            val item = target.definition.randomHandItem()
            when {
                item <= 0 -> Unit
                effects -> part.hooks.giveEffect(inner, unit, item, inner.now() + EFFECT_MILLIS)
                else -> part.hooks.handItemFromFurni(inner, unit, item)
            }
            val own = if (part.hooks.male(unit)) target.definition.effectMale else target.definition.effectFemale
            if (own > 0) part.hooks.giveEffect(inner, unit, own, null)
        }
        part.later(scope, furni, RESET_MILLIS) { inner, target ->
            if (target.state == BUSY) StateShow.data(inner, target, IDLE)
        }
    }

    /** A standing customer who looks more than one direction away turns to the machine; one seated or lying stays. */
    private fun faceMachine(
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        val anchor = furni.anchor ?: return
        if (unit.isWalking || unit.showsStatus(UnitStatus.SIT) || unit.showsStatus(UnitStatus.LAY)) return
        val direction = UnitDirection.between(unit.tile, anchor) ?: return
        val apart = Math.floorMod(unit.bodyDirection - direction, UnitDirection.COUNT)
        if (minOf(apart, UnitDirection.COUNT - apart) > 1) {
            unit.face(direction)
            unit.changed = true
        }
    }

    private companion object {
        const val IDLE = "0"
        const val BUSY = "1"
        const val HAND_ITEM_MILLIS = 1250L
        const val WATCH_TICKS = 60
        const val RESET_MILLIS = 1500L
        const val EFFECT_MILLIS = 30_000L
    }
}
