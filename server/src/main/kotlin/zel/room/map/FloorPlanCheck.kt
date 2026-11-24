package zel.room.map

object FloorPlanCheck {
    private const val MAX_DIRECTION = 7
    private val THICKNESS = -2..1
    private val WALL_HEIGHT = 0..15

    const val ENTRY_OUTSIDE_MAP = "entry_tile_outside_map"
    const val ENTRY_NOT_ON_TILE = "entry_not_on_tile"
    const val ENTRY_DIRECTION = "invalid_entry_tile_direction"
    const val WALL_THICKNESS = "invalid_wall_thickness"
    const val FLOOR_THICKNESS = "invalid_floor_thickness"
    const val WALLS_FIXED_HEIGHT = "invalid_walls_fixed_height"
    const val NO_FLOOR = "effective_height_is_0"
    const val BLOCKED_BY_FURNI = "change_blocked_by_room_item"
    const val TOO_LARGE = "too_large_area"

    fun check(
        request: FloorPlanRequest,
        floor: FloorPlan,
        occupied: List<Point>,
        currentWallHeight: Int = floor.fixedWallHeight,
        limits: FloorPlanLimits = FloorPlanLimits(),
    ): FloorPlanOutcome {
        val grid = readMap(request.map) ?: return FloorPlanOutcome.Dropped
        val wallHeight = request.fixedWallHeight ?: currentWallHeight
        val errors = valueErrors(request, grid, wallHeight.takeIf { request.fixedWallHeight != null }, limits)
        if (errors.isNotEmpty()) return FloorPlanOutcome.Refused(errors)
        blockedTile(grid, floor, occupied)?.let {
            return FloorPlanOutcome.Refused(listOf(FloorPlanError(BLOCKED_BY_FURNI, it)))
        }
        return FloorPlanOutcome.Accepted(grid, request.door, request.wallThickness, request.floorThickness, wallHeight)
    }

    fun readMap(map: String): TileGrid? {
        val rows = FloorMapText.rowsOf(map)
        val width = rows.firstOrNull()?.length ?: 0
        if (width == 0 || rows.any { it.length != width }) return null
        val heights = IntArray(width * rows.size)
        for ((y, row) in rows.withIndex()) {
            for ((x, c) in row.withIndex()) heights[y * width + x] = FloorMapText.heightOf(c) ?: return null
        }
        return TileGrid(width, rows.size, heights)
    }

    private fun valueErrors(
        request: FloorPlanRequest,
        grid: TileGrid,
        wallHeight: Int?,
        limits: FloorPlanLimits,
    ): List<FloorPlanError> {
        val door = request.door.tile
        val inside = door.x in 0 until grid.width && door.y in 0 until grid.height
        val values =
            listOfNotNull(
                ENTRY_OUTSIDE_MAP.takeIf { !inside },
                ENTRY_NOT_ON_TILE.takeIf { inside && grid.at(door) == FloorPlan.VOID },
                ENTRY_DIRECTION.takeIf { request.door.direction !in 0..MAX_DIRECTION },
                WALL_THICKNESS.takeIf { request.wallThickness !in THICKNESS },
                FLOOR_THICKNESS.takeIf { request.floorThickness !in THICKNESS },
                WALLS_FIXED_HEIGHT.takeIf { wallHeight != null && wallHeight !in WALL_HEIGHT },
                NO_FLOOR.takeIf { limits.needsFloor && !grid.hasFloor() },
            ).map { FloorPlanError(it) }
        return listOfNotNull(tooLarge(grid, limits)) + values
    }

    private fun tooLarge(
        grid: TileGrid,
        limits: FloorPlanLimits,
    ): FloorPlanError? {
        val limit =
            when {
                grid.width * grid.height > limits.maxArea -> limits.maxArea
                grid.heights.count { it != FloorPlan.VOID } > limits.maxOpenTiles -> limits.maxOpenTiles
                else -> return null
            }
        return FloorPlanError(TOO_LARGE, maxTiles = limit)
    }

    private fun blockedTile(
        grid: TileGrid,
        floor: FloorPlan,
        occupied: List<Point>,
    ): Point? =
        occupied.firstOrNull { tile ->
            val now = grid.at(tile)
            now == FloorPlan.VOID || now != (floor.baseHeight(tile) ?: FloorPlan.VOID)
        }
}
