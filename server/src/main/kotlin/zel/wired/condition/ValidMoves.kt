package zel.wired.condition

import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing

class ValidMoves(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val probe: MoveProbe,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean = probe.completes(firing, furniId)
}
