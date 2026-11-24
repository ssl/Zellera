package zel.game.players.inventory

import org.slf4j.LoggerFactory
import zel.db.Params
import zel.db.SqlFailure
import zel.game.items.OwnedFurni
import zel.game.items.OwnedFurniRows
import zel.game.items.ownerGuard
import zel.game.items.placement
import zel.server.GameServices
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

private val log = LoggerFactory.getLogger(InventoryFurniStore::class.java)

private const val BATCH_SIZE = 100

/** Excludes what Build Pass and playlists hold. */
private const val INVENTORY_SQL =
    "SELECT furni.* FROM furni LEFT JOIN build_pass_furni ON build_pass_furni.furni_id = furni.id " +
        "LEFT JOIN room_playlist_discs ON room_playlist_discs.furni_id = furni.id " +
        "WHERE furni.room_id IS NULL AND furni.player_id = ? AND build_pass_furni.furni_id IS NULL " +
        "AND room_playlist_discs.furni_id IS NULL"

internal class InventoryFurniStore(
    private val services: GameServices,
) {
    fun read(playerId: Int): Map<Int, OwnedFurni> =
        try {
            services.database.queries
                .select(INVENTORY_SQL) { int(playerId) }
                .result(::furniById)
        } catch (e: SqlFailure) {
            log.error("inventory of player {} not read", playerId, e)
            emptyMap()
        }

    fun writeChanged(furni: Collection<OwnedFurni>) {
        val changed = furni.filter { it.changed }
        if (changed.isEmpty()) return
        try {
            services.database.dataSource.connection.use { connection ->
                connection.prepareStatement(SAVE_SQL).use { statement ->
                    changed.chunked(BATCH_SIZE).forEach { save(statement, it) }
                }
            }
        } catch (e: SQLException) {
            log.error("{} changed inventory furni not saved", changed.size, e)
        }
    }

    private fun save(
        statement: PreparedStatement,
        chunk: List<OwnedFurni>,
    ) {
        val owners = chunk.map { it.ownerToSave() }
        chunk.zip(owners).forEach { (furni, owner) ->
            bindSave(statement, furni, owner)
            statement.addBatch()
        }
        try {
            statement.executeBatch()
        } catch (failure: SQLException) {
            chunk.forEach(OwnedFurni::markChanged)
            throw failure
        }
        chunk.zip(owners).forEach { (furni, owner) -> furni.markSaved(owner) }
    }

    private fun furniById(result: ResultSet): Map<Int, OwnedFurni> {
        val definitions = checkNotNull(services.game.items)
        val byId = LinkedHashMap<Int, OwnedFurni>()
        while (result.next()) {
            val id = result.getInt("id")
            try {
                val furni = definitions.readFurni(result)
                if (furni != null) byId[id] = furni else log.error("inventory furni {} has no item", id)
            } catch (e: SQLException) {
                log.error("inventory furni {} not read", id, e)
            }
        }
        return byId
    }

    companion object {
        const val SAVE_SQL = OwnedFurniRows.PLACEMENT_SET + OwnedFurniRows.OWNER_GUARD

        /** The owner guard keeps a stale copy from overwriting a traded-away furni. */
        @Throws(SQLException::class)
        fun bindSave(
            statement: PreparedStatement,
            furni: OwnedFurni,
            owner: Int = furni.userId,
        ) {
            Params(statement).apply {
                placement(furni, owner)
                ownerGuard(furni, owner)
            }
        }
    }
}
