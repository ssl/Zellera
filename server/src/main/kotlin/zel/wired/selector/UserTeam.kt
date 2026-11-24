package zel.wired.selector

import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

class UserTeam(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val facts: SelectorFacts,
) : UnitSelector() {
    override fun matches(firing: Firing): Collection<Int> {
        val wanted = settings.intAt(0)
        return firing.scope.room.units
            .all()
            .filter { unit ->
                val team = facts.team(firing.scope, unit)
                if (wanted == ANY) team != NO_TEAM else team == wanted
            }.map { it.index }
    }

    private companion object {
        const val ANY = 0
        const val NO_TEAM = 0
    }
}
