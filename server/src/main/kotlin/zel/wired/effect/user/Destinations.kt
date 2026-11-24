package zel.wired.effect.user

import zel.room.furni.RoomFurni
import zel.wired.box.WiredBox
import zel.wired.firing.Firing
import zel.wired.firing.FurniSource
import zel.wired.firing.Targets
import kotlin.math.absoluteValue

object Destinations {
    fun of(
        firing: Firing,
        box: WiredBox,
    ): List<RoomFurni> {
        val noFurni =
            box.settings.furniSources.firstOrNull() == FurniSource.TRIGGERING && firing.triggeringFurni == null
        val furni =
            if (noFurni) {
                box.settings.picks.mapNotNull { firing.scope.room.furni[it.absoluteValue] }
            } else {
                Targets.furni(firing, box)
            }
        return furni.filter { it.anchor != null }
    }
}
