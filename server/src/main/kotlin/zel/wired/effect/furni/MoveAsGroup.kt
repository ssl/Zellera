package zel.wired.effect.furni

import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class MoveAsGroup(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val moves: FurniMoves,
    private val walls: WallMoves,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val members = members(firing)
        val target = target(firing) ?: return
        if (members.isEmpty()) return
        val dx = target.x + offset(OFFSET_X) - members.minOf { it.tile.x }
        val dy = target.y + offset(OFFSET_Y) - members.minOf { it.tile.y }
        if (dx == 0 && dy == 0) return
        members.sortedByDescending { dx * it.tile.x + dy * it.tile.y }.forEach { member ->
            val tile = Point(member.tile.x + dx, member.tile.y + dy)
            if (member.furni.isWall) {
                walls.moveTo(
                    firing,
                    member.furni,
                    tile,
                )
            } else {
                moves.move(firing, member.furni, tile)
            }
        }
    }

    private fun members(firing: Firing): List<Member> =
        Targets.furni(firing, this, picksForTriggering = false).mapNotNull { furni ->
            tileOf(furni)?.let { Member(furni, it) }
        }

    private fun target(firing: Firing): Point? =
        if ((settings.ints.getOrNull(KIND) ?: KIND_FURNI) == KIND_FURNI) {
            Targets.furni(firing, this, slot = 1).firstOrNull()?.let(::tileOf)
        } else {
            Targets.units(firing, this).firstOrNull()?.tile
        }

    private fun offset(index: Int): Int = (settings.ints.getOrNull(index) ?: 0).coerceIn(-MAX_OFFSET, MAX_OFFSET)

    private fun tileOf(furni: RoomFurni): Point? = furni.anchor ?: WallPlace.of(furni)?.tile

    private class Member(
        val furni: RoomFurni,
        val tile: Point,
    )

    private companion object {
        const val KIND = 0
        const val KIND_FURNI = 0
        const val OFFSET_X = 1
        const val OFFSET_Y = 2
        const val MAX_OFFSET = 64
    }
}
