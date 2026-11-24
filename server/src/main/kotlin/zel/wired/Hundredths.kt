package zel.wired

import zel.room.furni.place.FloorLanding
import kotlin.math.roundToLong

/** Wired reads, compares and writes heights in hundredths of a tile; [clamped] caps at the wired ceiling. */
object Hundredths {
    private const val PER_TILE = 100.0

    val CEILING: Long = (FloorLanding.WIRED_CEILING * PER_TILE).roundToLong()

    fun of(z: Double): Long = (z * PER_TILE).roundToLong()

    fun clamped(z: Double): Long = clamp(of(z))

    fun clamp(value: Long): Long = value.coerceIn(0, CEILING)

    fun toTiles(value: Long): Double = value / PER_TILE
}
