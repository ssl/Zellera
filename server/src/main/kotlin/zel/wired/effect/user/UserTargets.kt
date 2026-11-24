package zel.wired.effect.user

import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.wired.box.WiredBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

object UserTargets {
    /** Every resolved unit: avatars, bots and pets (a pet selected by name can be moved or frozen too). */
    fun units(
        firing: Firing,
        box: WiredBox,
        slot: Int = 0,
    ): List<RoomUnit> = Targets.units(firing, box, slot)

    fun players(
        firing: Firing,
        box: WiredBox,
        slot: Int = 0,
    ): List<Pair<RoomUnit, Int>> =
        Targets.units(firing, box, slot).mapNotNull { unit ->
            (unit.kind as? UnitKind.Player)?.let { unit to it.playerId }
        }
}
