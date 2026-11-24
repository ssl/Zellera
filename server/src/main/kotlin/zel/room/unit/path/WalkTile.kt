package zel.room.unit.path

data class WalkTile(
    val state: TileState,
    val height: Double,
) {
    companion object {
        val VOID = WalkTile(TileState.Void, 0.0)
    }
}
