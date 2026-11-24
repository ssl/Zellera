package zel.room.map

sealed interface FloorPlanOutcome {
    data object Dropped : FloorPlanOutcome

    data class Refused(
        val errors: List<FloorPlanError>,
    ) : FloorPlanOutcome

    data class Accepted(
        val grid: TileGrid,
        val door: Door,
        val wallThickness: Int,
        val floorThickness: Int,
        val fixedWallHeight: Int,
    ) : FloorPlanOutcome
}
