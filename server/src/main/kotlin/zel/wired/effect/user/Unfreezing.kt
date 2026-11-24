package zel.wired.effect.user

import zel.room.RoomScope
import zel.room.furni.behaviour.state.StateHooks
import zel.room.games.GamesPart
import zel.room.unit.RoomUnit
import zel.wired.firing.Firing

/**
 * Freeze and unfreeze. Inside a firing both change the unit's state at once, but what the users see (the effect,
 * the stop) goes out once at the firing's end from the net change; an unfreeze of the same firing wins over its
 * freeze whatever the effect order, so freeze plus unfreeze leaves the user free and sends nothing.
 */
class Unfreezing(
    private val state: UserEffectState,
    private val hooks: StateHooks,
    private val games: GamesPart,
) {
    fun freeze(
        firing: Firing,
        unit: RoomUnit,
        frozen: Frozen,
    ) {
        net(firing).touch(unit)
        state.of(firing.scope.room).frozen[unit.index] = frozen
        games.hold(unit)
    }

    fun unfreeze(
        firing: Firing,
        unit: RoomUnit,
    ) {
        val net = net(firing)
        net.touch(unit)
        net.unfrozen += unit
        if (state.of(firing.scope.room).frozen.remove(unit.index) != null) games.release(unit)
    }

    /** Outside a firing (a teleport that ends the freeze): at once. */
    fun unfreeze(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        val frozen = state.of(scope.room).frozen.remove(unit.index) ?: return
        games.release(unit)
        forgetUnderTeleport(scope, unit, frozen.effect)
        endFreezeEffect(scope, unit, frozen.effect)
    }

    /** The freeze effect comes off; a team effect that waited during the freeze takes its place. */
    private fun endFreezeEffect(
        scope: RoomScope,
        unit: RoomUnit,
        freezeEffect: Int,
    ) {
        val waited = games.takeWaitingEffect(unit)
        if (waited != null) {
            hooks.giveEffect(scope, unit, waited, null)
        } else if (hooks.effect(unit) == freezeEffect) {
            hooks.giveEffect(scope, unit, 0, null)
        }
    }

    /** A teleport that started while frozen must not give the freeze effect back after its landing. */
    private fun forgetUnderTeleport(
        scope: RoomScope,
        unit: RoomUnit,
        effect: Int,
    ) {
        val ending = state.of(scope.room).teleportEffects[unit.index] ?: return
        if (ending.before == effect) ending.before = 0
    }

    /** Freezes and unfreezes net out over every stack of the event that shares the flash, sent when it goes out. */
    private fun net(firing: Firing): Net {
        val nets = state.of(firing.scope.room).freezeNets
        val flash = firing.flash
        return nets.getOrPut(flash) {
            Net(firing.scope).also { net ->
                flash.later {
                    nets.remove(flash)
                    net.settle()
                }
            }
        } as Net
    }

    private inner class Net(
        private val scope: RoomScope,
    ) {
        private val before = LinkedHashMap<RoomUnit, Frozen?>()
        val unfrozen = HashSet<RoomUnit>()

        fun touch(unit: RoomUnit) {
            if (unit !in before) before[unit] = state.of(scope.room).frozen[unit.index]
        }

        fun settle() {
            val frozen = state.of(scope.room).frozen
            before.forEach { (unit, was) ->
                if (scope.room.units[unit.index] !== unit) return@forEach
                if (unit in unfrozen && frozen.remove(unit.index) != null) games.release(unit)
                val now = frozen[unit.index]
                when {
                    now != null && now.effect != was?.effect -> {
                        // Only a walk stops with a line; a standing user gets the effect alone.
                        if (unit.isWalking) {
                            unit.path = emptyList()
                            unit.changed = true
                        }
                        hooks.giveEffect(scope, unit, now.effect, null)
                    }

                    now == null && was != null -> {
                        forgetUnderTeleport(scope, unit, was.effect)
                        endFreezeEffect(scope, unit, was.effect)
                    }
                }
            }
        }
    }
}
