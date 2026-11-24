package zel.game.games.snowball.arena

data class ArenaDefinition(
    val fieldType: Int,
    val name: String,
    val width: Int,
    val height: Int,
    val heightmap: List<String>,
    val fuse: List<FuseObject>,
    val trees: List<TreeRef>,
    val piles: List<PileRef>,
    val machines: List<MachineRef>,
    val spawns: Map<Int, List<Tile>>,
) {
    /** [height] and [altitude] are game units (1600 = one room height step); a background carries its picture. */
    data class FuseObject(
        val id: Int,
        val name: String,
        val x: Int,
        val y: Int,
        val xDim: Int,
        val yDim: Int,
        val dir: Int,
        val height: Int,
        val altitude: Int,
        val canStandOn: Boolean,
        val state: String,
        val background: Background?,
    )

    data class Background(
        val file: String,
        val offsetX: Int,
        val offsetY: Int,
        val offsetZ: Int,
    )

    data class TreeRef(
        val fuse: Int,
        val maxHits: Int,
        val hits: Int,
    )

    data class PileRef(
        val fuse: Int,
        val max: Int,
        val count: Int,
    )

    data class MachineRef(
        val fuse: Int,
        val max: Int,
        val count: Int,
    )

    data class Tile(
        val x: Int,
        val y: Int,
    )

    fun heightmapString(): String = heightmap.joinToString("\r")

    fun fuseObject(id: Int): FuseObject? = fuse.firstOrNull { it.id == id }

    fun spawns(teamId: Int): List<Tile> = spawns[teamId].orEmpty()

    companion object {
        const val BACKGROUND_FURNI = "ads_background"
    }
}
