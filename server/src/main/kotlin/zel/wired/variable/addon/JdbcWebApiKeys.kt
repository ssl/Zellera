package zel.wired.variable.addon

import zel.db.rows
import zel.db.write
import zel.room.furni.buildpass.BuildPassRowIds
import javax.sql.DataSource

class JdbcWebApiKeys(
    private val dataSource: DataSource,
) : WebApiKeyStore {
    override fun keys(boxId: Int): Map<String, StoredApiKey> =
        dataSource.connection.use { c ->
            val row = BuildPassRowIds.rowOf(c, boxId)
            c
                .rows(READ, { int(row) }) {
                    it.getString("kind") to
                        StoredApiKey(it.getBytes("key_hash"), it.getString("key_hint"), it.getBytes("key_sealed"))
                }.toMap()
        }

    override fun save(
        boxId: Int,
        kind: String,
        roomId: Int,
        userId: Int,
        key: StoredApiKey,
        createdAt: Long,
    ) {
        dataSource.connection.use { c ->
            val row = BuildPassRowIds.rowOf(c, boxId)
            c.write(SAVE) {
                int(row)
                string(kind)
                int(roomId)
                int(userId)
                int(row)
                value(key.hash)
                string(key.hint)
                value(key.sealed)
                long(createdAt)
            }
        }
    }

    override fun delete(
        boxId: Int,
        kind: String,
    ) {
        dataSource.connection.use { c ->
            val row = BuildPassRowIds.rowOf(c, boxId)
            c.write("DELETE FROM wired_api_keys WHERE box_id = ? AND kind = ?") {
                int(row)
                string(kind)
            }
        }
    }

    override fun deleteAll(boxId: Int) {
        dataSource.connection.use { c ->
            val row = BuildPassRowIds.rowOf(c, boxId)
            c.write("DELETE FROM wired_api_keys WHERE box_id = ?") { int(row) }
        }
    }

    private companion object {
        const val READ = "SELECT kind, key_hash, key_hint, key_sealed FROM wired_api_keys WHERE box_id = ?"
        const val SAVE =
            "INSERT INTO wired_api_keys (box_id, kind, room_id, player_id, key_hash, key_hint, key_sealed, " +
                "created_at_ms) VALUES (?, ?, ?, COALESCE((SELECT id FROM players WHERE id = ?), " +
                "(SELECT player_id FROM build_pass_furni WHERE furni_id = ?)), ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE room_id = VALUES(room_id), " +
                "player_id = VALUES(player_id), key_hash = VALUES(key_hash), key_hint = VALUES(key_hint), " +
                "key_sealed = VALUES(key_sealed), created_at_ms = VALUES(created_at_ms)"
    }
}
