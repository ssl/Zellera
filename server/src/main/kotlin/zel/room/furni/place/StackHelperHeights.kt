package zel.room.furni.place

import zel.room.Room
import zel.room.furni.FurniKind
import zel.room.furni.RoomFurni
import kotlin.math.floor

/** Pure reads of the room under its lock. */
object StackHelperHeights {
    const val ABOVE_STACK = -100

    private const val HUNDRED = 100.0
    private const val HALF = 0.5
    private const val EPSILON = 1e-9

    fun slider(
        room: Room,
        helper: RoomFurni,
        hundredths: Int,
    ): Double {
        val floor = floorOf(room, helper)
        if (hundredths == ABOVE_STACK) {
            val top =
                helper
                    .tiles()
                    .mapNotNull {
                        room.furni
                            .at(it)
                            .topItem()
                            ?.top
                    }.maxOrNull() ?: floor
            return minOf(maxOf(top, floor), FloorLanding.STACK_CEILING)
        }
        return (hundredths / HUNDRED).coerceIn(floor, maxOf(floor, FloorLanding.STACK_CEILING))
    }

    fun arrow(
        room: Room,
        helper: RoomFurni,
        down: Boolean,
    ): Double {
        val floor = floorOf(room, helper)
        val surfaces = surfaces(room, helper, floor)
        val current = helper.z
        if (surfaces.size == 1) return floor
        if (down) return surfaces.lastOrNull { it < current - EPSILON } ?: floor
        surfaces.firstOrNull { it > current + EPSILON }?.let { return minOf(it, FloorLanding.STACK_CEILING) }
        val highest = surfaces[surfaces.size - 1]
        val gap = highest - surfaces[surfaces.size - 2]
        val steps = maxOf(1.0, floor((current + gap - highest) / gap + HALF))
        return minOf(highest + steps * gap, FloorLanding.STACK_CEILING)
    }

    private fun surfaces(
        room: Room,
        helper: RoomFurni,
        floor: Double,
    ): List<Double> =
        helper
            .tiles()
            .flatMap { room.furni.at(it).furni }
            .distinct()
            .filter(::isSurface)
            .flatMap { listOf(it.z, it.top) }
            .plus(floor)
            .filter { it >= floor }
            .sorted()
            .fold(mutableListOf<Double>()) { list, h ->
                if (list.isEmpty() || h - list.last() > EPSILON) list += h
                list
            }

    private fun isSurface(furni: RoomFurni): Boolean =
        furni.kind != FurniKind.StackHelper &&
            furni.kind != FurniKind.WalkTile &&
            furni.isStackable &&
            !furni.definition.canSit

    private fun floorOf(
        room: Room,
        helper: RoomFurni,
    ): Double = helper.anchor?.let(room.floor::baseHeight)?.toDouble() ?: 0.0
}
