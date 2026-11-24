package zel.room.furni

import zel.room.map.Point

/**
 * The side a floor furni's picture faces, from its drawn directions (`direction_mask`) and footprint.
 * Four directions: it faces its rotation. One direction or two adjacent: x+1 or y+1 (a long furni, its long side).
 * Unknown art: the rotation.
 */
object DrawnFront {
    private const val NORTH = 0
    private const val EAST = 2
    private const val SOUTH = 4
    private const val WEST = 6
    private const val COUNT = 8
    private const val PAIR_STEP = 2

    fun facing(furni: RoomFurni): Int {
        val drawn = furni.definition.drawnDirections
        val shown = furni.definition.shownDirection(furni.direction)
        val first = viewerArt(drawn) ?: return shown
        val (w, l) = size(furni)
        return when {
            w > l -> SOUTH
            l > w -> EAST
            shown == first -> EAST
            else -> SOUTH
        }
    }

    /** The tile in front of the anchor on the drawn front, just outside the footprint; null for a wall furni. */
    fun tile(furni: RoomFurni): Point? {
        val anchor = furni.anchor ?: return null
        val (w, l) = size(furni)
        return when (val facing = facing(furni)) {
            NORTH -> Point(anchor.x, anchor.y - 1)
            EAST -> Point(anchor.x + w, anchor.y)
            SOUTH -> Point(anchor.x, anchor.y + l)
            WEST -> Point(anchor.x - 1, anchor.y)
            else -> anchor.step(facing)
        }
    }

    /** For art that shows only the viewer's two sides, the direction drawn facing x+1; null for other art. */
    private fun viewerArt(drawn: List<Int>): Int? =
        when (drawn.size) {
            1 -> drawn[0]
            2 -> drawn.firstOrNull { Math.floorMod(it + PAIR_STEP, COUNT) in drawn }
            else -> null
        }

    private fun size(furni: RoomFurni): Pair<Int, Int> {
        val anchor = furni.anchor ?: return 1 to 1
        val tiles = furni.tiles()
        return (tiles.maxOf { it.x } - anchor.x + 1) to (tiles.maxOf { it.y } - anchor.y + 1)
    }
}
