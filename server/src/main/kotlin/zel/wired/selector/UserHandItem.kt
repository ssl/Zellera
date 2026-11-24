package zel.wired.selector

import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

class UserHandItem(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val facts: SelectorFacts,
) : UnitSelector() {
    override fun matches(firing: Firing): Collection<Int> {
        val item = maxOf(0, settings.intAt(0, FRESH_ITEM))
        return firing.scope.room.units
            .all()
            .filter { facts.handItem(it) == item }
            .map { it.index }
    }

    companion object {
        const val FRESH_ITEM = 2
    }
}
