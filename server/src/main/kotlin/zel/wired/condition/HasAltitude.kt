package zel.wired.condition

import zel.wired.Hundredths
import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class HasAltitude(
    override val furniId: Int,
    override val settings: BoxSettings,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val altitude = settings.intAt(0).toLong()
        val comparison = Comparison.radio(settings.intAt(1))
        val targets = Targets.furni(firing, this)
        if (targets.isEmpty()) return Quantified.noFurni(this)
        return Quantified.holds(targets, settings) {
            Comparison.holds(comparison, Hundredths.clamped(it.z), altitude)
        }
    }
}
