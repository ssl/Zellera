package zel.wired.selector

import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

class UserByName(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val facts: SelectorFacts,
) : UnitSelector() {
    private val names =
        settings.text
            .lineSequence()
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()

    /** Avatars, bots and pets: a pet is selected by its name too. */
    override fun matches(firing: Firing): Collection<Int> =
        firing.scope.room.units
            .all()
            .filter { unit -> facts.unitName(firing.scope, unit)?.lowercase() in names }
            .map { it.index }
}
