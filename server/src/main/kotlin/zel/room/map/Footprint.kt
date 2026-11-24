package zel.room.map

import zel.room.furni.RoomFurni

object Footprint {
    private const val EAST = 2
    private const val WEST = 6

    fun tiles(
        anchor: Point,
        width: Int,
        length: Int,
        direction: Int,
    ): List<Point> {
        val turned = direction == EAST || direction == WEST
        val w = maxOf(1, if (turned) length else width)
        val l = maxOf(1, if (turned) width else length)
        return List(w * l) { Point(anchor.x + it % w, anchor.y + it / w) }
    }

    fun of(furni: RoomFurni): List<Point> =
        furni.anchor
            ?.let { tiles(it, furni.definition.width, furni.definition.length, furni.direction) }
            .orEmpty()

    fun occupied(furni: Collection<RoomFurni>): List<Point> = furni.flatMap(::of).distinct()
}
