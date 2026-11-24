package zel.wired.selector

import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

class UserOnFurni(
    override val furniId: Int,
    override val settings: BoxSettings,
) : UnitSelector() {
    override fun matches(firing: Firing): Collection<Int> {
        val tiles = SelectorSources.furni(firing, this).flatMapTo(HashSet()) { it.tiles() }
        return firing.scope.room.units
            .all()
            .filter { it.tile in tiles }
            .map { it.index }
    }
}
