package zel.wired.selector

import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

class UserNeighbourhood(
    override val furniId: Int,
    override val settings: BoxSettings,
) : UnitSelector() {
    override fun matches(firing: Firing): Collection<Int> {
        val centres = NeighbourhoodCentres.of(firing, this)
        if (centres.isEmpty()) return emptyList()
        return firing.scope.room.units
            .all()
            .filter { unit ->
                centres.any { NeighbourhoodGrid.chosen(settings.ints, unit.tile.x - it.x, unit.tile.y - it.y) }
            }.map { it.index }
    }
}
