package zel.game.icons

import zel.db.allOrNothing
import zel.db.first
import zel.db.rows
import zel.db.write
import zel.db.writeEach
import zel.game.icons.IconRoster.Price
import zel.game.icons.IconRoster.StoredIcon
import zel.game.icons.IconRoster.StoredSet
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

internal class IconStore(
    private val dataSource: DataSource,
) {
    inner class PlayerRows(
        private val connection: Connection,
        private val playerId: Int,
    ) {
        /** Icons come in `sort_order` within each set, which is not always id order. */
        fun roster(allUnlocked: Boolean): IconRoster =
            IconRoster.assemble(
                fetch(SELECT_ICONS, ::storedIcon, playerId),
                fetch(SELECT_SETS, ::storedSet),
                fetch(SELECT_LAST_USED, { it.getInt(1) }, playerId),
                allUnlocked,
            )

        fun saveState(
            iconId: Int,
            state: IconState,
            unseen: Boolean,
        ) {
            connection.write(UPSERT_STATE) {
                int(playerId)
                int(iconId)
                int(state.code)
                boolean(unseen)
            }
        }

        /** Makes [iconId] the player's most recent: its time is later than any of theirs, even in one clock tick. */
        fun markUsed(
            iconId: Int,
            atMillis: Long,
        ) {
            connection.write(MARK_USED) {
                long(atMillis)
                int(playerId)
                int(playerId)
                int(iconId)
            }
        }

        private fun <T> fetch(
            sql: String,
            read: (ResultSet) -> T,
            vararg ids: Int,
        ): List<T> = connection.rows(sql, { ids.forEach(::int) }, read)
    }

    private fun storedIcon(rows: ResultSet): StoredIcon {
        val id = rows.getInt("id")
        val savedCode = rows.getInt("state").takeUnless { rows.wasNull() }
        return StoredIcon(
            id = id,
            name = rows.getString("name"),
            setId = rows.getInt("collection_id"),
            price = price(rows),
            saved = savedCode?.let(IconState::fromCode),
            unseen = rows.getBoolean("unseen"),
            ownedByDefault = rows.getBoolean("owned_by_default"),
            available = rows.getBoolean("available"),
            isSetReward = rows.getInt("reward_id") == id,
        )
    }

    private fun storedSet(rows: ResultSet) =
        StoredSet(rows.getInt("id"), rows.getString("name"), rows.getInt("reward_id"), price(rows))

    private fun price(rows: ResultSet) =
        Price(rows.getInt("price_credits"), rows.getInt("price_currency"), rows.getInt("currency"))

    fun <T> read(
        playerId: Int,
        body: (PlayerRows) -> T,
    ): T = dataSource.connection.use { body(PlayerRows(it, playerId)) }

    /** Runs [body] in one transaction, locking the player's row so two changes of one player go one after the other. */
    fun <T : Any> exclusively(
        playerId: Int,
        body: (PlayerRows) -> T,
    ): T =
        dataSource.connection.use { connection ->
            checkNotNull(
                connection.allOrNothing {
                    lockPlayer(connection, playerId)
                    body(PlayerRows(connection, playerId))
                },
            )
        }

    private fun lockPlayer(
        connection: Connection,
        playerId: Int,
    ) {
        connection.first("SELECT id FROM players WHERE id = ? FOR UPDATE", { int(playerId) }) { true }
            ?: throw IconRejected(IconRejected.UNKNOWN)
    }

    /** Clears the unseen mark of [iconIds], or of every icon of the player when the list is empty. */
    fun markSeen(
        playerId: Int,
        iconIds: List<Int>,
    ) {
        dataSource.connection.use { connection ->
            if (iconIds.isEmpty()) {
                connection.write(SEEN_ALL) { int(playerId) }
            } else {
                connection.writeEach(SEEN_ONE, iconIds) {
                    int(playerId)
                    int(it)
                }
            }
        }
    }

    private companion object {
        const val SELECT_SETS = "SELECT * FROM icon_collections ORDER BY id"
        const val SELECT_ICONS = """
            SELECT i.id, i.collection_id, i.name, i.available, i.owned_by_default,
                i.price_credits, i.price_currency, i.currency, c.reward_id, o.state, o.unseen
            FROM icons i
            JOIN icon_collections c ON c.id = i.collection_id
            LEFT JOIN player_icons o ON o.icon_id = i.id AND o.player_id = ?
            ORDER BY i.collection_id, i.sort_order, i.id
            """
        const val SELECT_LAST_USED = """
            SELECT icon_id FROM player_icons WHERE player_id = ? AND last_used_ms > 0 AND state IN (2, 3)
            ORDER BY last_used_ms DESC, icon_id DESC LIMIT 10
            """
        const val UPSERT_STATE = """
            INSERT INTO player_icons (player_id, icon_id, state, unseen) VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE state = VALUES(state), unseen = VALUES(unseen) OR unseen
            """
        const val MARK_USED = """
            UPDATE player_icons SET last_used_ms =
                GREATEST(?, (SELECT COALESCE(MAX(recent.last_used_ms), 0) + 1
                    FROM player_icons recent WHERE recent.player_id = ?))
            WHERE player_id = ? AND icon_id = ?
            """
        const val SEEN_ALL = "UPDATE player_icons SET unseen = FALSE WHERE player_id = ?"
        const val SEEN_ONE = "UPDATE player_icons SET unseen = FALSE WHERE player_id = ? AND icon_id = ?"
    }
}
