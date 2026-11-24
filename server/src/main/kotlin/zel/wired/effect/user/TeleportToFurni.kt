package zel.wired.effect.user

import zel.room.unit.UnitKind
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets
import kotlin.random.Random

class TeleportToFurni(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val teleports: Teleports,
    private val random: Random,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val furni = Destinations.of(firing, this)
        if (furni.isEmpty()) return
        val fast = (settings.ints.firstOrNull() ?: 0) != 0
        Targets.units(firing, this, 0).filter { it.kind !is UnitKind.Pet }.forEach { unit ->
            teleports.start(firing.scope, unit, furni.random(random), fast)
        }
    }
}
