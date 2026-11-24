package zel.wired.selector

import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

class FurniSignal(
    override val furniId: Int,
    override val settings: BoxSettings,
) : FurniSelector() {
    override fun unconfigured(firing: Firing): Boolean = firing.signal == null

    override fun matches(firing: Firing): Collection<Int> = firing.signal?.furniIds.orEmpty()
}
