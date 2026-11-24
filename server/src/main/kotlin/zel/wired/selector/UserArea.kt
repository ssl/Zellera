package zel.wired.selector

import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

class UserArea(
    override val furniId: Int,
    override val settings: BoxSettings,
) : UnitSelector() {
    private val area = Area.of(settings)

    override fun matches(firing: Firing): Collection<Int> =
        firing.scope.room.units
            .all()
            .filter { it.tile in area }
            .map { it.index }
}
