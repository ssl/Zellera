package zel.room.furni

class TileStack(
    val furni: List<RoomFurni>,
) {
    fun topItem(exclude: RoomFurni? = null): RoomFurni? =
        furni.filter { it !== exclude && !it.isHeightSetter() }.maxWithOrNull(TOP_ORDER)

    /**
     * The furni a unit steps on or off on this tile (walks-on / walks-off and step behaviours): the top item with walk
     * tiles counted, which [topItem] leaves out for stacking; a walk tile above other furni is the one stepped on. The
     * top item counts at any height above the unit, and a walk tile under it never does. None when a stack helper is
     * on the tile, whatever the top item's height: then nothing of the tile is stepped on.
     */
    fun stepItem(): RoomFurni? {
        if (helperZ() != null) return null
        return furni.maxWithOrNull(TOP_ORDER)
    }

    fun helperZ(exclude: RoomFurni? = null): Double? =
        furni.filter { it !== exclude && it.kind == FurniKind.StackHelper }.maxOfOrNull { it.z }

    fun walkTileZ(): Double? = furni.filter { it.kind == FurniKind.WalkTile }.maxOfOrNull { it.z }

    /**
     * Where a furni lands on this tile, or null when nothing can go on it: the highest helper's z, else the top of the
     * top level, which takes nothing when any furni of that level is not stackable (whatever the ids).
     */
    fun stackingHeight(
        floor: Double,
        exclude: RoomFurni? = null,
    ): Double? {
        helperZ(exclude)?.let { return it }
        val level = topLevel(furni.filter { it !== exclude && !it.isHeightSetter() }.map(::Layer)) ?: return floor
        return if (level.stackable) level.top else null
    }

    /** The HeightMap value; a walk tile counts here as a 0-high stackable furni at its z, a helper sets the value. */
    fun heightMapValue(floor: Double): Short {
        helperZ()?.let { return encode(it) }
        val level = mapLevel() ?: return encode(floor)
        return if (level.stackable) encode(level.top) else (BLOCKED or units(level.z)).toShort()
    }

    /** The height [heightMapValue] stands for, unrounded: on a tile that takes nothing, the z of its top level. */
    fun heightMapZ(floor: Double): Double {
        helperZ()?.let { return it }
        val level = mapLevel() ?: return floor
        return if (level.stackable) level.top else level.z
    }

    private fun mapLevel(): Layer? =
        topLevel(
            furni.filter { it.kind != FurniKind.StackHelper }.map {
                if (it.kind == FurniKind.WalkTile) Layer.walkTile(it) else Layer(it)
            },
        )

    /** A furni as stacking sees it: its top, its z and whether it takes furni on top. */
    private class Layer(
        val top: Double,
        val z: Double,
        val stackable: Boolean,
    ) {
        constructor(furni: RoomFurni) : this(furni.top, furni.z, furni.isStackable)

        companion object {
            fun walkTile(furni: RoomFurni) = Layer(furni.z, furni.z, true)
        }
    }

    companion object {
        val EMPTY = TileStack(emptyList())

        const val BLOCKED = 0x8000

        private const val UNITS_PER_TILE = 256
        private const val MAX_UNITS = 0x7FFF

        /**
         * The order that picks a tile's top item (the greatest wins). Tops and z compare as single-precision floats: a
         * 0-high furni at z 25.2 and a 0.2-high one at z 25 have one top.
         */
        val TOP_ORDER: Comparator<RoomFurni> =
            compareBy<RoomFurni> { level(it.top) }.thenBy { level(it.z) }.thenBy { it.id }

        /** A top or z compared in single precision. */
        fun level(height: Double): Float = height.toFloat()

        fun encode(height: Double): Short = units(height).toShort()

        private fun units(height: Double): Int = (height * UNITS_PER_TILE).toInt().coerceIn(0, MAX_UNITS)

        private fun RoomFurni.isHeightSetter(): Boolean = kind == FurniKind.StackHelper || kind == FurniKind.WalkTile

        /**
         * The top level of [layers] (highest top, then highest z, both in [level] precision) as one layer, stackable
         * only when every layer of it is: on a tie a furni that takes nothing blocks the tile, whichever was placed
         * first.
         */
        private fun topLevel(layers: List<Layer>): Layer? {
            val top = layers.maxOfOrNull { level(it.top) } ?: return null
            val atTop = layers.filter { level(it.top) == top }
            val z = atTop.maxOf { level(it.z) }
            val tied = atTop.filter { level(it.z) == z }
            return Layer(tied.maxOf { it.top }, tied.maxOf { it.z }, tied.all { it.stackable })
        }
    }
}
