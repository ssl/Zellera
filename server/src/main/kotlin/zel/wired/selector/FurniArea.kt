package zel.wired.selector

import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

class FurniArea(
    override val furniId: Int,
    override val settings: BoxSettings,
) : FurniSelector() {
    private val area = Area.of(settings)

    override fun matches(firing: Firing): Collection<Int> =
        firing.scope.room.furni
            .all()
            .filter { furni -> !furni.isWall && furni.tiles().any(area::contains) }
            .map { it.id }
}
