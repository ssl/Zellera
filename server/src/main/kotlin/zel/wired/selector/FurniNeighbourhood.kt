package zel.wired.selector

import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

class FurniNeighbourhood(
    override val furniId: Int,
    override val settings: BoxSettings,
) : FurniSelector() {
    override fun matches(firing: Firing): Collection<Int> {
        val centres = NeighbourhoodCentres.of(firing, this)
        if (centres.isEmpty()) return emptyList()
        return firing.scope.room.furni
            .all()
            .filter { furni ->
                !furni.isWall &&
                    furni.tiles().any { tile ->
                        centres.any { NeighbourhoodGrid.chosen(settings.ints, tile.x - it.x, tile.y - it.y) }
                    }
            }.map { it.id }
    }
}
