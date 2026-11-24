package zel.wired.effect.user

import zel.room.games.ClockAction
import zel.room.games.GamesPart
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class ControlClock(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val games: GamesPart,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val action = ClockAction.fromCode(settings.ints.getOrNull(0) ?: 0) ?: return
        Targets.furni(firing, this).forEach { furni ->
            games.clockOf(furni)?.let { games.clocks.control(firing.scope, furni, it, action) }
        }
    }
}
