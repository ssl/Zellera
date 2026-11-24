package zel.room.map

data class FloorPlanError(
    val key: String,
    val tile: Point? = null,
    val maxTiles: Int? = null,
)
