package zel.wired.effect.furni

import zel.room.map.Point
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class RelativeMove(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val moves: FurniMoves,
    private val walls: WallMoves,
) : EffectBox,
    TriesMoves {
    override val negative = false
    override val needsUser = false

    override fun tryMoves(
        firing: Firing,
        trial: MoveTrial,
    ) {
        val dx = settings.ints.getOrNull(0) ?: 0
        val dy = settings.ints.getOrNull(1) ?: 0
        if (dx == 0 && dy == 0) return
        Targets.furni(firing, this).filterNot { it.isWall }.forEach { furni ->
            furni.anchor?.let { trial.move(furni, Point(it.x + dx, it.y + dy)) }
        }
    }

    override fun run(firing: Firing) {
        val dx = settings.ints.getOrNull(0) ?: 0
        val dy = settings.ints.getOrNull(1) ?: 0
        if (dx == 0 && dy == 0) return
        Targets.furni(firing, this).forEach { furni ->
            val anchor = furni.anchor
            if (anchor != null) {
                moves.move(firing, furni, Point(anchor.x + dx, anchor.y + dy))
            } else {
                WallPlace.of(furni)?.let {
                    walls.moveTo(firing, furni, Point(it.tile.x + dx, it.tile.y + dy))
                }
            }
        }
    }
}
