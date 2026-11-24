package zel.wired.selector

import zel.room.unit.UnitKind
import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

class UserByType(
    override val furniId: Int,
    override val settings: BoxSettings,
) : UnitSelector() {
    override fun matches(firing: Firing): Collection<Int> {
        val type = settings.intAt(0, AVATARS)
        return firing.scope.room.units
            .all()
            .filter { unit ->
                when (type) {
                    PETS -> unit.kind is UnitKind.Pet
                    BOTS -> unit.kind is UnitKind.Bot
                    else -> unit.kind is UnitKind.Player
                }
            }.map { it.index }
    }

    private companion object {
        const val AVATARS = 1
        const val PETS = 2
        const val BOTS = 4
    }
}
