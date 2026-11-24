package zel.wired.selector

import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

class FurniPicks(
    override val furniId: Int,
    override val settings: BoxSettings,
) : FurniSelector() {
    override val wallItems get() = true

    override fun matches(firing: Firing): Collection<Int> = SelectorSources.furni(firing, this).map { it.id }
}
