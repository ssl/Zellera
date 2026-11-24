package zel.wired.effect.furni

import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets
import kotlin.random.Random

class MoveRotate(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val moves: FurniMoves,
    private val walls: WallMoves,
    private val random: Random,
) : EffectBox,
    TriesMoves {
    override val negative = false
    override val needsUser = false

    override fun tryMoves(
        firing: Firing,
        trial: MoveTrial,
    ) {
        val movement = settings.ints.getOrNull(0) ?: 0
        val rotation = settings.ints.getOrNull(1) ?: 0
        Targets.furni(firing, this).filterNot { it.isWall }.forEach { furni ->
            val anchor = furni.anchor ?: return@forEach
            val step = step(movement)
            val tile = step?.let { Point(anchor.x + it.dx, anchor.y + it.dy) } ?: anchor
            if (step != null || rotation in ROTATIONS) trial.move(furni, tile, Turns.next(furni, rotation, random))
        }
    }

    override fun run(firing: Firing) {
        val movement = settings.ints.getOrNull(0) ?: 0
        val rotation = settings.ints.getOrNull(1) ?: 0
        Targets.furni(firing, this).forEach { furni ->
            val step = step(movement)
            if (step != null || rotation in ROTATIONS) {
                if (furni.isWall) wall(firing, furni, step, rotation) else floor(firing, furni, step, rotation)
            }
        }
    }

    private fun floor(
        firing: Firing,
        furni: RoomFurni,
        step: Step?,
        rotation: Int,
    ) {
        val anchor = furni.anchor ?: return
        val tile = step?.let { Point(anchor.x + it.dx, anchor.y + it.dy) } ?: anchor
        moves.move(firing, furni, tile, Turns.next(furni, rotation, random), turnSlides = true)
    }

    private fun wall(
        firing: Firing,
        item: RoomFurni,
        step: Step?,
        rotation: Int,
    ) {
        val place = WallPlace.of(item) ?: return
        step?.let { walls.moveTo(firing, item, Point(place.tile.x + it.dx, place.tile.y + it.dy)) }
        // A wall item has two sides: a random turn always takes the other one.
        if (rotation in ROTATIONS) walls.turn(firing, item)
    }

    private fun step(movement: Int): Step? =
        when (movement) {
            RANDOM_ANY -> STEPS.random(random)
            RANDOM_X -> X_STEP.random(random)
            RANDOM_Y -> Y_STEP.random(random)
            else -> if (movement < FIRST_FIXED) null else STEPS.getOrElse(movement - FIRST_FIXED) { SOUTH }
        }

    private data class Step(
        val dx: Int,
        val dy: Int,
    )

    private companion object {
        const val RANDOM_ANY = 1
        const val RANDOM_X = 2
        const val RANDOM_Y = 3
        const val FIRST_FIXED = 4
        val ROTATIONS = Turns.CLOCKWISE..Turns.RANDOM
        val NORTH = Step(0, -1)
        val EAST = Step(1, 0)
        val SOUTH = Step(0, 1)
        val WEST = Step(-1, 0)
        val X_STEP = listOf(EAST, WEST)
        val Y_STEP = listOf(NORTH, SOUTH)

        /** Movements 4..11, in order (movement 1 draws one of them); any higher value steps south, a negative one nothing. */
        val STEPS = listOf(NORTH, EAST, SOUTH, WEST, Step(1, -1), Step(1, 1), Step(-1, 1), Step(-1, -1))
    }
}
