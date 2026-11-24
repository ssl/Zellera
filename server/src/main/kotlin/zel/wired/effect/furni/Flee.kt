package zel.wired.effect.furni

import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class Flee(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val moves: FurniMoves,
    private val standsOn: (RoomScope, RoomUnit) -> Point,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        Targets.furni(firing, this).filterNot { it.isWall }.forEach { stepAway(firing, it) }
    }

    private val lastStep = HashMap<Int, Int>()

    /**
     * Straight away from the player it sees, else to the side; a player right next to it has caught it: it stays.
     * Seeing nobody it goes on the way it last went (east at first), turning counter-clockwise where that is shut.
     */
    private fun stepAway(
        firing: Firing,
        furni: RoomFurni,
    ) {
        val anchor = furni.anchor ?: return
        val room = firing.scope.room
        val seen = LineSight.from(room, anchor) { standsOn(firing.scope, it) }
        if (seen != null && seen.distance == 1) {
            firing.scope.raise(RoomEvent.Collision(seen.unit.index, furni.id))
            return
        }
        val ways =
            if (seen != null) {
                LineSight.opposite(seen.direction).let { listOf(it) + LineSight.sides(it) }
            } else {
                val last = lastStep[furni.id] ?: EAST
                (0 until TURNS).map { (last + it * COUNTER_CLOCKWISE) % UnitDirection.COUNT }
            }
        val way = ways.firstOrNull { moves.landing(room, furni, anchor.step(it)) != null } ?: return
        if (moves.move(firing, furni, anchor.step(way))) lastStep[furni.id] = way
    }

    private companion object {
        const val EAST = 2
        const val TURNS = 4
        const val COUNTER_CLOCKWISE = 6
    }
}
