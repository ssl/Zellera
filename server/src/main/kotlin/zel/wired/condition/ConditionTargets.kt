package zel.wired.condition

import zel.wired.box.WiredBox
import zel.wired.firing.FurniSource

internal object ConditionTargets {
    fun sourceOf(
        box: WiredBox,
        slot: Int,
    ): Int = box.settings.furniSources.getOrNull(slot) ?: FurniSource.PICKED
}
