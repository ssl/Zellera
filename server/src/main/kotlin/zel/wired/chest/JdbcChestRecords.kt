package zel.wired.chest

import zel.db.Params
import zel.db.first
import zel.db.rows
import zel.db.write
import zel.room.furni.buildpass.BuildPassRowIds
import zel.wired.chest.ChestRows.ROW_ID
import zel.wired.chest.ChestRows.tiny
import zel.wired.store.writeRowByRow
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

internal class JdbcChestRecords(
    private val dataSource: DataSource,
) {
    fun ofRoom(roomId: Int): Map<Int, ChestRecord> =
        dataSource.connection.use { c ->
            val furni =
                c
                    .rows(ROOM_FURNI, { int(roomId) }) { it.getInt("id") to furni(it) }
                    .groupBy({ it.first }, { it.second })
            c
                .rows(ROOM_CHESTS, { int(roomId) }) { rows ->
                    val id = rows.getInt("id")
                    id to record(rows, furni[id].orEmpty())
                }.toMap()
        }

    fun of(furniId: Int): ChestRecord? =
        dataSource.connection.use { c ->
            val clientId: Params.() -> Unit = {
                int(furniId)
                int(furniId)
            }
            val furni = c.rows(ONE_FURNI, clientId, ::furni)
            c.first(ONE_CHEST, clientId) { record(it, furni) }
        }

    private fun record(
        rows: ResultSet,
        furni: List<StoredFurni>,
    ) = ChestRecord(
        preferences =
            ChestPreferences(
                name = rows.getString("name").orEmpty(),
                description = rows.getString("description").orEmpty(),
                everyoneOpens = rows.getBoolean("access_open"),
                everyoneDonates = rows.getBoolean("access_donate"),
                lid = rows.getInt("appearance"),
                previewMode = rows.getInt("preview_mode"),
                previewAmount = rows.getInt("preview_amount"),
                wiredEnabled = rows.getBoolean("wired_enabled"),
            ),
        notifications =
            ChestNotifications(
                full = rows.getBoolean("notify_full"),
                donation = rows.getBoolean("notify_donation"),
                withdraw = rows.getBoolean("notify_withdraw"),
                empty = rows.getBoolean("notify_empty"),
                wired = rows.getBoolean("notify_wired"),
                mode = rows.getInt("notify_mode"),
            ),
        locked = rows.getBoolean("locked"),
        autoLock = rows.getBoolean("auto_lock"),
        capacity = rows.getInt("capacity"),
        capacityMax = rows.getInt("capacity_max"),
        placed = rows.getBoolean("placed"),
        credits = rows.getInt("credits"),
        furni = furni,
    )

    private fun furni(rows: ResultSet) =
        StoredFurni(
            slotId = rows.getInt("slot_id"),
            transactionId = rows.getLong("transaction_id"),
            isWall = rows.getBoolean("is_wall"),
            definitionId = rows.getInt("definition_id"),
            spriteId = rows.getInt("sprite_id"),
            posterCode = rows.getString("poster_code").orEmpty(),
            groupable = rows.getBoolean("groupable"),
            state = rows.getString("state").orEmpty(),
            limitedNumber = rows.getInt("limited_number"),
            limitedTotal = rows.getInt("limited_total"),
            extra = rows.getInt("extra"),
            furniId = rows.getInt("furni_id"),
        )

    /** Inside the caller's transaction. */
    fun save(
        c: Connection,
        furniId: Int,
        chest: ChestRecord,
    ) {
        val rowId = BuildPassRowIds.rowOf(c, furniId)
        c.write(SAVE_CHEST) { chest(rowId, chest) }
        c.write(DELETE_FURNI) { int(rowId) }
        c.writeRowByRow(SAVE_FURNI, chest.furni) { furni ->
            int(furni.slotId)
            long(furni.transactionId)
            int(furni.definitionId)
            int(furni.spriteId)
            boolean(furni.isWall)
            boolean(furni.groupable)
            string(furni.state)
            string(furni.posterCode)
            int(furni.limitedNumber)
            int(furni.limitedTotal)
            int(furni.extra)
            int(furni.furniId)
            int(rowId)
        }
    }

    private fun Params.chest(
        rowId: Int,
        chest: ChestRecord,
    ) {
        val look = chest.preferences
        val notify = chest.notifications
        string(look.name.take(ChestPreferences.MAX_NAME))
        string(look.description.take(ChestPreferences.MAX_DESCRIPTION))
        int(chest.credits)
        int(chest.capacity)
        int(chest.capacityMax)
        int(tiny(look.lid))
        int(tiny(look.previewMode))
        int(look.previewAmount)
        boolean(chest.placed)
        boolean(look.everyoneOpens)
        boolean(look.everyoneDonates)
        boolean(chest.locked)
        boolean(chest.autoLock)
        boolean(look.wiredEnabled)
        int(tiny(notify.mode))
        boolean(notify.full)
        boolean(notify.donation)
        boolean(notify.withdraw)
        boolean(notify.empty)
        boolean(notify.wired)
        int(rowId)
    }

    private companion object {
        const val CHEST_COLUMNS =
            "c.name, c.description, c.credits, c.capacity, c.capacity_max, c.appearance, c.preview_mode, " +
                "c.preview_amount, c.placed, c.access_open, c.access_donate, c.locked, c.auto_lock, c.wired_enabled, " +
                "c.notify_mode, c.notify_full, c.notify_donation, c.notify_withdraw, c.notify_empty, c.notify_wired"
        const val FURNI_COLUMNS =
            "f.slot_id, f.transaction_id, f.definition_id, f.sprite_id, f.is_wall, f.groupable, f.state, " +
                "f.poster_code, f.limited_number, f.limited_total, f.extra, f.furni_id"
        const val ROOM_CHESTS =
            "SELECT ${BuildPassRowIds.CLIENT_FACING} AS id, $CHEST_COLUMNS FROM wired_chests c " +
                "JOIN furni i ON i.id = c.furni_id ${BuildPassRowIds.JOIN} WHERE i.room_id = ?"
        const val ROOM_FURNI =
            "SELECT ${BuildPassRowIds.CLIENT_FACING} AS id, $FURNI_COLUMNS FROM wired_chest_furni f " +
                "JOIN furni i ON i.id = f.chest_id ${BuildPassRowIds.JOIN} WHERE i.room_id = ? " +
                "ORDER BY f.chest_id, f.slot_id"
        const val ONE_CHEST = "SELECT $CHEST_COLUMNS FROM wired_chests c WHERE c.furni_id = $ROW_ID"
        const val ONE_FURNI =
            "SELECT $FURNI_COLUMNS FROM wired_chest_furni f WHERE f.chest_id = $ROW_ID ORDER BY f.slot_id"
        const val SAVE_CHEST =
            "INSERT INTO wired_chests (furni_id, name, description, credits, capacity, capacity_max, appearance, " +
                "preview_mode, preview_amount, placed, access_open, access_donate, locked, auto_lock, wired_enabled, " +
                "notify_mode, notify_full, notify_donation, notify_withdraw, notify_empty, notify_wired) " +
                "SELECT i.id, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? " +
                "FROM furni i WHERE i.id = ? ON DUPLICATE KEY UPDATE name = VALUES(name), " +
                "description = VALUES(description), credits = VALUES(credits), capacity = VALUES(capacity), " +
                "capacity_max = VALUES(capacity_max), appearance = VALUES(appearance), " +
                "preview_mode = VALUES(preview_mode), preview_amount = VALUES(preview_amount), " +
                "placed = VALUES(placed), access_open = VALUES(access_open), access_donate = VALUES(access_donate), " +
                "locked = VALUES(locked), auto_lock = VALUES(auto_lock), wired_enabled = VALUES(wired_enabled), " +
                "notify_mode = VALUES(notify_mode), notify_full = VALUES(notify_full), " +
                "notify_donation = VALUES(notify_donation), notify_withdraw = VALUES(notify_withdraw), " +
                "notify_empty = VALUES(notify_empty), notify_wired = VALUES(notify_wired)"
        const val DELETE_FURNI = "DELETE FROM wired_chest_furni WHERE chest_id = ?"
        const val SAVE_FURNI =
            "INSERT INTO wired_chest_furni (slot_id, transaction_id, definition_id, sprite_id, is_wall, groupable, " +
                "state, poster_code, limited_number, limited_total, extra, furni_id, chest_id) " +
                "SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, (SELECT i.id FROM furni i WHERE i.id = ?), c.furni_id " +
                "FROM wired_chests c WHERE c.furni_id = ?"
    }
}
