package zel.wired.selector

import zel.wired.Hundredths
import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

class FurniAltitude(
    override val furniId: Int,
    override val settings: BoxSettings,
) : FurniSelector() {
    override fun matches(firing: Firing): Collection<Int> {
        val altitude = Hundredths.clamp(settings.intAt(0).toLong())
        val comparison = settings.intAt(1)
        return firing.scope.room.furni
            .all()
            .filter { furni ->
                val z = Hundredths.clamped(furni.z)
                !furni.isWall &&
                    when (comparison) {
                        BELOW -> z < altitude
                        ABOVE -> z > altitude
                        else -> z == altitude
                    }
            }.map { it.id }
    }

    private companion object {
        const val BELOW = 0
        const val ABOVE = 2
    }
}
