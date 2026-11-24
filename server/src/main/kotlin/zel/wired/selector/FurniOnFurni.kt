package zel.wired.selector

import zel.room.furni.RoomFurni
import zel.wired.Hundredths
import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

class FurniOnFurni(
    override val furniId: Int,
    override val settings: BoxSettings,
) : FurniSelector() {
    override fun matches(firing: Firing): Collection<Int> {
        val room = firing.scope.room
        val mode = settings.intAt(0)
        return SelectorSources
            .furni(firing, this)
            .filterNot { it.isWall }
            .flatMap { source ->
                source
                    .tiles()
                    .flatMap { room.furni.at(it).furni }
                    .distinct()
                    .filter { fits(mode, source, it) }
            }.map { it.id }
    }

    private fun fits(
        mode: Int,
        source: RoomFurni,
        other: RoomFurni,
    ): Boolean =
        when (mode) {
            BELOW -> other !== source && Hundredths.clamped(other.top) <= Hundredths.clamped(source.z)
            SAME_HEIGHT -> Hundredths.clamped(other.z) == Hundredths.clamped(source.z)
            EVERYTHING -> true
            else -> other !== source && Hundredths.clamped(other.z) >= Hundredths.clamped(source.top)
        }

    private companion object {
        const val BELOW = 1
        const val SAME_HEIGHT = 2
        const val EVERYTHING = 3
    }
}
