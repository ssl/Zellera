package zel.wired.effect.furni

import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import kotlin.random.Random

class MoveFurniTo(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val moves: FurniMoves,
    private val random: Random,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    private val distances = HashMap<Int, Int>()

    override fun run(firing: Firing) {
        val room = firing.scope.room
        val furni = firing.triggeringFurni?.let { room.furni[it] }?.takeUnless(RoomFurni::isWall) ?: return
        val pick =
            settings.picks
                .mapNotNull { room.furni[it] }
                .filterNot { it === furni || it.isWall }
                .randomOrNull(random)
                ?: return
        val origin = pick.anchor ?: return
        val direction = settings.ints.getOrNull(DIRECTION) ?: 0
        val span = span(room.floor.width, room.floor.height)
        val spacing = (settings.ints.getOrNull(SPACING) ?: 1).coerceIn(1, span)
        val distance = distances[pick.id] ?: 0
        val line = generateSequence(origin) { it.step(direction) }.take(distance + 1).takeWhile(room.floor::contains)
        val spot =
            line
                .withIndex()
                .lastOrNull()
                ?.takeIf { it.index == distance }
                ?.value
        val tile: Point = if (spot != null && moves.landing(room, furni, spot) != null) spot else origin
        val travelled = if (tile == spot) distance else 0
        moves.move(firing, furni, tile)
        distances[pick.id] = (travelled + spacing).coerceAtMost(span)
    }

    companion object {
        private const val DIRECTION = 0
        const val SPACING = 1

        fun span(
            width: Int,
            height: Int,
        ): Int = maxOf(width, height, 1)
    }
}
