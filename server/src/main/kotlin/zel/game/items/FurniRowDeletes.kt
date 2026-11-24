package zel.game.items

import zel.db.allOrNothing
import zel.db.write
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

private const val DELETE_INVENTORY_ROW =
    "DELETE FROM furni WHERE id = ? AND player_id = ? AND (room_id IS NULL OR room_id = 0)"

/** One transaction; any missing furni rolls it back. */
@Throws(SQLException::class)
fun <T : Any> DataSource.exchangeInventoryFurni(
    ownerId: Int,
    taken: Collection<OwnedFurni>,
    make: (Connection) -> T?,
): T? =
    connection.use { connection ->
        connection.allOrNothing {
            val allDeleted =
                taken.all { item ->
                    connection.write(DELETE_INVENTORY_ROW) {
                        int(item.dbId)
                        int(ownerId)
                    } == 1
                }
            if (allDeleted) make(connection) else null
        }
    }
