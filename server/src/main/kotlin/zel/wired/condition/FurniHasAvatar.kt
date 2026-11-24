package zel.wired.condition

import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing
import zel.wired.firing.FurniSource
import zel.wired.firing.Targets

class FurniHasAvatar(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val negative: Boolean,
    private val units: UnitFacts,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val fromTrigger = ConditionTargets.sourceOf(this, 0) == FurniSource.TRIGGERING
        if (fromTrigger && firing.triggeringFurni == null) return true
        val standing =
            firing.scope.room.units
                .all()
                .map { units.arrivedAt(firing.scope, it) }
                .toSet()
        val targets = Targets.furni(firing, this)
        // An empty set: the not box is the positive one turned around.
        if (targets.isEmpty() && Quantified.noFurniPass(this)) {
            return Quantified.emptyHolds(settings.intAt(0) == ALL, negative)
        }
        val positive =
            Quantified.perTarget(targets, settings.intAt(0) == ALL, false) { furni ->
                furni.tiles().any { it in standing }
            }
        return positive != negative
    }

    private companion object {
        const val ALL = 1
    }
}
