package zel.wired.selector

import zel.room.unit.UnitKind
import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

class UserGroup(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val facts: SelectorFacts,
) : UnitSelector() {
    override val avatarsOnly get() = true

    override fun matches(firing: Firing): Collection<Int> {
        val groupId = if (settings.intAt(0) == CHOSEN) settings.intAt(1) else firing.scope.room.info.groupId
        if (groupId <= 0) return emptyList()
        return firing.scope.room.units
            .all()
            .filter { it.kind is UnitKind.Player && facts.inGroup(firing.scope, it, groupId) }
            .map { it.index }
    }

    private companion object {
        const val CHOSEN = 1
    }
}
