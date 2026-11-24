package zel.room.map

import zel.core.rules.rules

data class FloorPlanLimits(
    val needsFloor: Boolean = rules.floorPlanRequiresFloor,
    val maxArea: Int = rules.floorPlanMaxArea,
    val maxOpenTiles: Int = rules.floorPlanMaxOpenTiles,
)
