package zel.wired.selector

import zel.room.furni.RoomFurni
import zel.room.unit.RoomUnit
import zel.wired.box.WiredBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

object SelectorSources {
    fun furni(
        firing: Firing,
        box: WiredBox,
    ): List<RoomFurni> = Targets.uncounted(firing) { Targets.furni(firing, box, picksForTriggering = false) }

    fun units(
        firing: Firing,
        box: WiredBox,
    ): List<RoomUnit> = Targets.uncounted(firing) { Targets.units(firing, box) }
}
