package zel.wired.addon

import zel.room.map.Footprint
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.wired.box.BoxSettings
import zel.wired.effect.furni.FloorMove
import zel.wired.effect.furni.FurniMoves
import zel.wired.effect.furni.MoveAddon
import zel.wired.firing.Firing
import zel.wired.firing.Targets
import zel.wired.timing.postureHeight

class MoveCarryUsers(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val carrier: UserCarrier,
) : MoveAddon {
    private val anyOnTiles = settings.intAt(0) == 1

    override fun landed(
        firing: Firing,
        move: FloorMove,
    ) {
        val dx = move.to.x - move.from.x
        val dy = move.to.y - move.from.y
        if (dx == 0 && dy == 0) return
        val furni = move.furni
        val old = Footprint.tiles(move.from, furni.definition.width, furni.definition.length, furni.direction)
        val allowed = Targets.units(firing, this).map { it.index }.toSet()
        val duration = MoveStyles.userDuration(firing)
        firing.scope.room.units
            .all()
            .filter { it.index in allowed && it.tile in old && !it.isWalking && (anyOnTiles || onIt(it, move)) }
            .forEach { carrier.carry(firing, it, Point(it.tile.x + dx, it.tile.y + dy), duration) }
    }

    /** Standing at the furni's old top, or seated/lying on it. */
    private fun onIt(
        unit: RoomUnit,
        move: FloorMove,
    ): Boolean = FurniMoves.onFurni(unit.z, unit.postureHeight, move.fromZ, move.fromZ + move.furni.height)
}
