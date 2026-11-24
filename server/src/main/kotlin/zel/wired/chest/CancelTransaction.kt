package zel.wired.chest

import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class CancelTransaction(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val deals: ChestDeals,
) : EffectBox {
    override val negative = false
    override val needsUser = true

    override fun run(firing: Firing) {
        val any = settings.intAt(0) != 0
        val picked = Targets.furni(firing, this, 0).map { it.id }.toSet()
        Targets.units(firing, this, 0).forEach { unit ->
            unit.playerId()?.let { player ->
                deals.cancelByWired(firing.scope, player) { any || it in picked }
            }
        }
    }
}
