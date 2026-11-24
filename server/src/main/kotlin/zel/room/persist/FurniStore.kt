package zel.room.persist

import zel.db.Sql
import zel.db.allOrNothing
import zel.db.intOrNone
import zel.db.write
import java.sql.ResultSet
import javax.sql.DataSource

class FurniStore(
    private val dataSource: DataSource,
) : FurniData,
    FurniTransfers {
    private val sql = Sql(dataSource)

    override fun roomFurni(roomId: Int): List<FurniRow> = sql.select(SELECT_ROOM, roomId).rows { it.furni() }

    override fun save(
        roomId: Int,
        changes: List<FurniSave>,
    ) {
        if (changes.isEmpty()) return
        sql.writeEach(UPDATE, changes) { save ->
            string(save.wallPosition)
            int(save.x)
            int(save.y)
            double(save.z)
            int(save.direction)
            string(save.state)
            int(save.id)
            int(roomId)
        }
    }

    override fun putInRoom(
        roomId: Int,
        ownerId: Int,
        place: FurniSave,
    ): Boolean =
        sql.write(PUT_IN_ROOM) {
            int(roomId)
            string(place.wallPosition)
            int(place.x)
            int(place.y)
            double(place.z)
            int(place.direction)
            string(place.state)
            int(place.id)
            int(ownerId)
        } == 1

    override fun takeToInventory(
        roomId: Int,
        furniId: Int,
        state: String,
    ): Boolean = sql.write(TAKE_TO_INVENTORY, state, furniId, roomId) == 1

    /** One transaction, rows in id order; gives the ids that really were in the room and moved. */
    override fun takeAllToInventory(
        roomId: Int,
        furni: List<FurniHandBack>,
    ): List<Int> =
        dataSource.connection.use { connection ->
            connection
                .allOrNothing {
                    furni.sortedBy { it.id }.filter { item ->
                        connection.write(TAKE_TO_INVENTORY) {
                            string(item.state)
                            int(item.id)
                            int(roomId)
                        } == 1
                    }
                }.orEmpty()
                .map { it.id }
        }

    companion object {
        private fun ResultSet.furni(): FurniRow =
            FurniRow(
                id = getInt("id"),
                ownerId = intOrNone("player_id"),
                ownerName = getString("owner_name").orEmpty(),
                definitionId = getInt("definition_id"),
                wallPosition = getString("wall_position").orEmpty(),
                x = getInt("x"),
                y = getInt("y"),
                z = getDouble("z"),
                direction = getInt("direction"),
                state = getString("state").orEmpty(),
                limitedData = "${getInt("limited_total")}:${getInt("limited_number")}",
                groupId = intOrNone("guild_id"),
                clientId = getInt("client_id").takeIf { it != 0 },
                wiredCopy = getInt("copy_id") != 0,
            )

        private const val OWNER_NAME =
            "COALESCE(u.name, (SELECT `value` FROM settings WHERE `name` = 'room.ownerless.owner_name'), '')"

        internal const val SELECT_ROOM =
            "SELECT i.id, i.player_id, $OWNER_NAME AS owner_name, i.definition_id, i.wall_position, i.x, i.y, i.z, " +
                "i.direction, i.state, i.limited_number, i.limited_total, i.guild_id, b.client_id, " +
                "w.furni_id AS copy_id FROM furni i LEFT JOIN players u ON u.id = i.player_id " +
                "LEFT JOIN build_pass_furni b ON b.furni_id = i.id " +
                "LEFT JOIN wired_spawned_furni w ON w.furni_id = i.id WHERE i.room_id = ? ORDER BY i.id"

        /** Only a row still in the room: a save queued before a pickup never writes room data into an inventory. */
        internal const val UPDATE =
            "UPDATE furni SET wall_position = ?, x = ?, y = ?, z = ?, direction = ?, state = ?" +
                " WHERE id = ? AND room_id = ?"

        internal const val PUT_IN_ROOM =
            "UPDATE furni SET room_id = ?, wall_position = ?, x = ?, y = ?, z = ?, direction = ?, state = ?" +
                " WHERE id = ? AND player_id = ? AND room_id IS NULL" +
                " AND NOT EXISTS (SELECT 1 FROM room_playlist_discs WHERE furni_id = furni.id)"

        internal const val TAKE_TO_INVENTORY =
            "UPDATE furni SET room_id = NULL, wall_position = '', x = 0, y = 0, z = 0, direction = 0, state = ?" +
                " WHERE id = ? AND room_id = ?"
    }
}
