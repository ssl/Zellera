package zel.wired.condition

import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.box.pattern.UserCountIn
import zel.wired.firing.Firing

class NotUserCount(
    override val furniId: Int,
    override val settings: BoxSettings,
) : ConditionBox {
    private val inside = UserCountIn(furniId, settings)

    override fun passes(firing: Firing): Boolean = !inside.passes(firing)
}
