@file:Suppress("MagicNumber") // heightmap letters and Direction8 indexes

package zel.game.games.snowball.sim

import zel.game.games.snowball.arena.ArenaDefinition.FuseObject

/** A Snowball arena's tile grid; every game builds its own because the simulation changes it. */
class ArenaStage(
    val width: Int,
    val height: Int,
    heightmap: String,
    fuseObjects: List<FuseObject>,
) {
    private val tiles = Array(height) { arrayOfNulls<ArenaTile>(width) }

    init {
        val values = parseHeightmap(heightmap)
        for (y in 0 until height) {
            for (x in 0 until width) {
                // a cell past a short row's end is a tile; a missing row is skipped (ArenaRepository refuses those)
                if (y >= values.size || (x < values[y].size && values[y][x] == NO_TILE)) {
                    continue
                }
                val tile = ArenaTile(x, y)
                tiles[y][x] = tile
                linkIfPresent(tile, x + 1, y - 1, 1)
                linkIfPresent(tile, x, y - 1, 0)
                linkIfPresent(tile, x - 1, y - 1, 7)
                linkIfPresent(tile, x - 1, y, 6)
            }
        }
        for (fuse in fuseObjects) {
            val origin = tile(fuse.x, fuse.y) ?: continue
            origin.addFuseObject(fuse)
            markFootprint(fuse)
        }
    }

    private fun linkIfPresent(
        tile: ArenaTile,
        x: Int,
        y: Int,
        direction8: Int,
    ) {
        tile(x, y)?.let { tile.link(it, direction8) }
    }

    private fun markFootprint(fuse: FuseObject) {
        val swapped = fuse.dir == 2 || fuse.dir == 6
        val xDim = if (swapped) fuse.yDim else fuse.xDim
        val yDim = if (swapped) fuse.xDim else fuse.yDim
        for (i in 1 until xDim) {
            markFootprintTile(tile(fuse.x + i, fuse.y), fuse)
        }
        for (i in 1 until yDim) {
            markFootprintTile(tile(fuse.x, fuse.y + i), fuse)
        }
    }

    private fun markFootprintTile(
        tile: ArenaTile?,
        fuse: FuseObject,
    ) {
        if (tile == null) {
            return
        }
        tile.addHeight(fuse.height)
        if (!fuse.canStandOn) {
            tile.block()
        }
    }

    fun tile(
        x: Int,
        y: Int,
    ): ArenaTile? = if (x < 0 || y < 0 || y >= height || x >= width) null else tiles[y][x]

    internal fun resetTiles() {
        for (row in tiles) {
            for (tile in row) {
                tile?.reset()
            }
        }
    }

    companion object {
        internal const val NO_TILE = 100_000

        internal fun parseHeightmap(heightmap: String): Array<IntArray> =
            heightmap
                .split('\r')
                .map { row ->
                    IntArray(row.length) { x ->
                        val c = row[x]
                        when {
                            c in '0'..'9' -> c - '0'
                            c == 'x' -> NO_TILE
                            else -> 10 + (c - 'a')
                        }
                    }
                }.toTypedArray()

        fun worldToTile(world: Int): Int = (world + ArenaTile.WORLD_SIZE / 2) / ArenaTile.WORLD_SIZE
    }
}
