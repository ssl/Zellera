package zel.room.map

import zel.db.Sql
import zel.db.allOrNothing
import zel.db.write
import zel.room.RoomVisuals
import java.sql.ResultSet
import javax.sql.DataSource

class FloorStore(
    private val dataSource: DataSource,
) : FloorData {
    private val sql = Sql(dataSource)

    override fun models(): List<RoomModel> =
        sql.select(SELECT_MODELS).rows { row ->
            RoomModel(name = row.getString("name"), map = row.getString("heightmap"), door = row.door())
        }

    override fun settings(roomId: Int): FloorSettings? = sql.select(SELECT_SETTINGS, roomId).first { it.settings() }

    override fun customFloor(roomId: Int): CustomFloor? =
        sql.select(SELECT_CUSTOM, roomId).first { row ->
            CustomFloor(map = row.getString("heightmap"), door = row.door())
        }

    override fun saveFloorPlan(
        roomId: Int,
        floor: CustomFloor,
        visuals: RoomVisuals,
        fixedWallHeight: Int,
    ) {
        dataSource.connection.use { c ->
            c.allOrNothing {
                c.write(UPSERT_CUSTOM) {
                    int(roomId)
                    string("custom_$roomId")
                    int(floor.door.tile.x)
                    int(floor.door.tile.y)
                    int(floor.door.direction)
                    string(floor.map)
                }
                c.write(UPDATE_ROOM) {
                    int(visuals.wallThickness)
                    int(visuals.floorThickness)
                    int(fixedWallHeight)
                    int(roomId)
                }
            }
        }
    }

    companion object {
        private fun ResultSet.door(): Door = Door(Point(getInt("door_x"), getInt("door_y")), getInt("door_direction"))

        private fun ResultSet.settings(): FloorSettings =
            FloorSettings(customFloor = getBoolean("has_custom_floor"), fixedWallHeight = getInt("wall_height"))

        internal const val SELECT_MODELS = "SELECT name, door_x, door_y, door_direction, heightmap FROM room_models"
        internal const val SELECT_SETTINGS = "SELECT has_custom_floor, wall_height FROM rooms WHERE id = ?"
        internal const val SELECT_CUSTOM =
            "SELECT door_x, door_y, door_direction, heightmap FROM room_floor_plans WHERE room_id = ?"
        internal const val UPSERT_CUSTOM =
            "INSERT INTO room_floor_plans (room_id, name, door_x, door_y, door_direction, heightmap)" +
                " VALUES (?, ?, ?, ?, ?, ?)" +
                " ON DUPLICATE KEY UPDATE door_x = VALUES(door_x), door_y = VALUES(door_y)," +
                " door_direction = VALUES(door_direction), heightmap = VALUES(heightmap)"
        internal const val UPDATE_ROOM =
            "UPDATE rooms SET has_custom_floor = TRUE, wall_thickness = ?, floor_thickness = ?, wall_height = ?" +
                " WHERE id = ?"
    }
}
