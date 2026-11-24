package zel.room.map

import zel.protocol.generated.FloorHeightMapOut
import zel.protocol.generated.HeightMapOut
import zel.protocol.generated.HeightMapUpdateOut
import zel.protocol.generated.RoomEntryTileOut
import zel.protocol.generated.RoomOccupiedTilesOut
import zel.protocol.generated.RoomVisualizationSettingsOut
import zel.room.RoomVisuals

object MapMessages {
    const val VOID_VALUE: Short = -1

    const val UPDATE_LIMIT = 127

    /** The map a client gets of a floor too large for it: no tiles. */
    private const val NO_FLOOR = "x\r"

    fun heightMap(
        floor: FloorPlan,
        valueOf: (Point) -> Short,
    ): HeightMapOut =
        if (floor.fitsClient) {
            HeightMapOut(floor.width, floor.tiles().map(valueOf))
        } else {
            HeightMapOut(1, listOf(VOID_VALUE))
        }

    /**
     * [areas]: the area hides that are on when the user enters, one entry each; the client hides their floor while it
     * builds the room. The camera starts on the entrant's own tile and z: the door, or the furni it arrived through.
     */
    fun floorHeightMap(
        floor: FloorPlan,
        areas: List<FloorHeightMapOut.List1Item> = emptyList(),
        camera: Point = floor.door.tile,
        cameraZ: Float = 0f,
    ): FloorHeightMapOut =
        FloorHeightMapOut(
            floor.smallScale,
            floor.fixedWallHeight,
            if (floor.fitsClient) FloorMapText.render(floor) else NO_FLOOR,
            areas,
            camera.x,
            camera.y,
            cameraZ,
        )

    fun entryTile(door: Door): RoomEntryTileOut = RoomEntryTileOut(door.tile.x, door.tile.y, door.direction)

    fun visualization(visuals: RoomVisuals): RoomVisualizationSettingsOut =
        RoomVisualizationSettingsOut(visuals.hideWalls, visuals.wallThickness, visuals.floorThickness)

    fun occupiedTiles(tiles: Collection<Point>): RoomOccupiedTilesOut =
        RoomOccupiedTilesOut(tiles.map { RoomOccupiedTilesOut.List1Item(it.x, it.y) })

    fun heightMapUpdates(
        tiles: List<Point>,
        before: (Point) -> Short,
        after: (Point) -> Short,
    ): List<HeightMapUpdateOut> =
        heightMapUpdates(tiles.mapNotNull { p -> after(p).takeIf { it != before(p) }?.let { TileValue(p, it) } })

    fun heightMapUpdates(changes: List<TileValue>): List<HeightMapUpdateOut> =
        changes.chunked(UPDATE_LIMIT).map { part ->
            HeightMapUpdateOut(
                part.map { HeightMapUpdateOut.List1Item(it.tile.x.toByte(), it.tile.y.toByte(), it.value) },
            )
        }
}
