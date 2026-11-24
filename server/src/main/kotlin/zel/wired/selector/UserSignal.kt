package zel.wired.selector

import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

class UserSignal(
    override val furniId: Int,
    override val settings: BoxSettings,
) : UnitSelector() {
    override fun unconfigured(firing: Firing): Boolean = firing.signal == null

    override fun matches(firing: Firing): Collection<Int> = firing.signal?.unitIndexes.orEmpty()
}
