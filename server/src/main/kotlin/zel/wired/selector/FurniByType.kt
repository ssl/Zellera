package zel.wired.selector

import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

class FurniByType(
    override val furniId: Int,
    override val settings: BoxSettings,
) : FurniSelector() {
    override val wallItems get() = true

    override fun matches(firing: Firing): Collection<Int> {
        val sources = SelectorSources.furni(firing, this)
        val sameState = settings.intAt(0) != 0
        return firing.scope.room.furni
            .all()
            .filter { furni ->
                sources.any { it.definition.id == furni.definition.id && (!sameState || it.state == furni.state) }
            }.map { it.id }
    }
}
