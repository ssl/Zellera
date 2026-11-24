package zel.game.players.inventory

import org.slf4j.LoggerFactory
import zel.db.SqlFailure
import zel.db.writeEach
import zel.game.players.PlayerSaveQueue
import zel.server.GameServices
import java.sql.SQLException
import java.util.TreeMap

private val log = LoggerFactory.getLogger(UnseenItemsDatabase::class.java)

private const val LOAD_SQL =
    "SELECT category, item_id FROM player_unseen_items WHERE player_id = ? ORDER BY category, seq"
private const val ADD_SQL = "INSERT IGNORE INTO player_unseen_items (player_id, category, item_id) VALUES (?, ?, ?)"
private const val REMOVE_SQL = "DELETE FROM player_unseen_items WHERE player_id = ? AND category = ? AND item_id = ?"
private const val CLEAR_SQL = "DELETE FROM player_unseen_items WHERE player_id = ? AND category = ?"

/** [writes] keeps each player's writes in order, so a reset never overtakes the insert it undoes. */
internal class UnseenItemsDatabase(
    private val services: GameServices,
    private val writes: PlayerSaveQueue,
) : UnseenItems.Store {
    override fun load(userId: Int): Map<Int, Set<Int>> =
        try {
            services.database.queries.select(LOAD_SQL) { int(userId) }.result { set ->
                val stored = TreeMap<Int, MutableSet<Int>>()
                while (set.next()) {
                    stored.getOrPut(set.getInt("category"), ::LinkedHashSet).add(set.getInt("item_id"))
                }
                stored
            }
        } catch (e: SqlFailure) {
            log.error("Could not load the unseen items of user {}", userId, e)
            TreeMap()
        }

    override fun add(
        userId: Int,
        category: Int,
        ids: Collection<Int>,
    ) = writeEach(ADD_SQL, userId, category, ids)

    override fun clear(
        userId: Int,
        category: Int,
    ) {
        writes.submit(userId) {
            logged(userId) {
                services.database.queries.write(CLEAR_SQL) {
                    int(userId)
                    int(category)
                }
            }
        }
    }

    override fun remove(
        userId: Int,
        category: Int,
        ids: Collection<Int>,
    ) = writeEach(REMOVE_SQL, userId, category, ids)

    private fun writeEach(
        sql: String,
        userId: Int,
        category: Int,
        ids: Collection<Int>,
    ) {
        val copy = ids.toList()
        writes.submit(userId) {
            logged(userId) {
                services.database.dataSource.connection.use { connection ->
                    connection.writeEach(sql, copy) {
                        int(userId)
                        int(category)
                        int(it)
                    }
                }
            }
        }
    }

    private fun logged(
        userId: Int,
        write: () -> Unit,
    ) {
        try {
            write()
        } catch (e: SQLException) {
            log.error("Could not update the unseen items of user {}", userId, e)
        }
    }
}
