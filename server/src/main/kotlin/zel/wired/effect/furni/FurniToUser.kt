package zel.wired.effect.furni

import zel.room.furni.RoomFurni
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class FurniToUser(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val moves: FurniMoves,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val unit = Targets.units(firing, this).firstOrNull() ?: return
        Targets
            .furni(firing, this)
            .filterNot { it.isWall }
            .sortedWith(compareBy<RoomFurni> { it.z }.thenBy { it.id })
            .forEach { furni -> moves.move(firing, furni, unit.tile, unitsBlock = false) }
    }
}
