package zel.wired.addon

import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.wired.box.BoxSettings
import zel.wired.effect.furni.MoveAddon
import zel.wired.effect.furni.MoveStyle
import zel.wired.effect.furni.MoveThrough
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class MovePhysics(
    override val furniId: Int,
    override val settings: BoxSettings,
) : MoveAddon {
    private val keepsAltitude = settings.intAt(KEEP_ALTITUDE, 1) != 0
    private val throughFurni = settings.intAt(THROUGH_FURNI) != 0
    private val throughUsers = settings.intAt(THROUGH_USERS) != 0
    private val blocking = settings.intAt(BLOCKED_BY_FURNI) == 1

    override fun style(
        firing: Firing,
        style: MoveStyle,
    ): MoveStyle = style.copy(keepsAltitude = keepsAltitude)

    override fun through(firing: Firing): MoveThrough? =
        if (throughFurni || throughUsers) {
            MoveThrough(
                if (throughFurni) furniOf(firing, THROUGH).map { it.id }.toSet() else emptySet(),
                if (throughUsers) Targets.units(firing, this).map { it.index }.toSet() else emptySet(),
            )
        } else {
            null
        }

    override fun refuses(
        firing: Firing,
        furni: RoomFurni,
        tiles: List<Point>,
    ): Boolean {
        if (!blocking) return false
        val room = firing.scope.room
        return furniOf(firing, BLOCKERS)
            .any { other -> other !== furni && other.tiles().any { it in tiles } && room.furni[other.id] != null }
    }

    private fun furniOf(
        firing: Firing,
        slot: Int,
    ): List<RoomFurni> = Targets.furni(firing, this, slot, filtered = false)

    private companion object {
        const val KEEP_ALTITUDE = 0
        const val THROUGH_FURNI = 1
        const val THROUGH_USERS = 2
        const val BLOCKED_BY_FURNI = 3
        const val THROUGH = 0
        const val BLOCKERS = 1
    }
}
