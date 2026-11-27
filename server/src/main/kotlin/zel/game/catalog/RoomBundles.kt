@file:Suppress("MagicNumber") // JDBC parameter positions

package zel.game.catalog

import org.slf4j.LoggerFactory
import zel.core.rules.rules
import zel.db.allOrNothing
import zel.db.insertForId
import zel.db.orFallback
import zel.db.rows
import zel.db.write
import zel.game.bots.BotChatLines
import zel.game.items.ProductKind
import zel.game.players.Player
import zel.protocol.outgoing.generic.alerts.BubbleAlert
import zel.protocol.outgoing.generic.alerts.BubbleAlertMessage
import zel.room.listing.ListedRoom
import java.sql.Connection

private val LOGGER = LoggerFactory.getLogger(RoomBundles::class.java)

private const val COPY_ROOM_SQL =
    "INSERT INTO rooms (owner_id, name, description, model_name, door_secret, door_mode, max_players, category_id, " +
        "floor_paper, wall_paper, landscape_paper, wall_thickness, floor_thickness, wall_height, hide_walls, " +
        "has_custom_floor, idle_kick, idle_kick_seconds, created_at) " +
        "(SELECT ?, name, description, model_name, door_secret, door_mode, max_players, " +
        "category_id, floor_paper, wall_paper, landscape_paper, wall_thickness, floor_thickness, wall_height, " +
        "hide_walls, has_custom_floor, ?, ?, UNIX_TIMESTAMP() FROM rooms WHERE id = ?)"

private const val COPY_DIMMER_SQL =
    "INSERT INTO room_dimmer_presets (room_id, position, preset, selected, background_only, color, brightness) " +
        "(SELECT ?, position, preset, selected, background_only, color, brightness FROM room_dimmer_presets " +
        "WHERE room_id = ?)"

private const val TEMPLATE_ITEMS_SQL =
    "SELECT id FROM furni WHERE room_id = ? AND definition_id NOT IN " +
        "(SELECT id FROM furni_definitions WHERE behaviour = 'wf_xtra_var_web_api') ORDER BY id"

private const val COPY_ITEM_SQL =
    "INSERT INTO furni (player_id, room_id, definition_id, wall_position, x, y, z, direction, state) " +
        "(SELECT ?, ?, definition_id, wall_position, x, y, z, direction, state FROM furni WHERE id = ?)"

private val COPY_WIRED_SQL =
    listOf(
        "INSERT INTO wired_boxes (furni_id, is_fresh, ints, text, furni_sources, player_sources, variable_ids, " +
            "quantifier, delay_half_seconds, filter_existing, invert, extras) (SELECT ?, is_fresh, ints, text, " +
            "furni_sources, player_sources, variable_ids, quantifier, delay_half_seconds, filter_existing, invert, " +
            "extras FROM wired_boxes WHERE furni_id = ?)",
        "INSERT INTO wired_box_picks (box_id, list, position, furni_id, saved_state, saved_x, saved_y, saved_z, " +
            "saved_direction, saved_wall_position) (SELECT ?, list, position, furni_id, saved_state, saved_x, " +
            "saved_y, saved_z, saved_direction, saved_wall_position FROM wired_box_picks WHERE box_id = ?)",
        "INSERT INTO wired_chests (furni_id, name, description, capacity, capacity_max, appearance, " +
            "preview_mode, preview_amount, placed, access_open, access_donate, locked, auto_lock, wired_enabled, " +
            "notify_mode, notify_full, notify_donation, notify_withdraw, notify_empty, notify_wired) " +
            "(SELECT ?, name, description, capacity, capacity_max, appearance, preview_mode, " +
            "preview_amount, placed, access_open, access_donate, locked, auto_lock, wired_enabled, notify_mode, " +
            "notify_full, notify_donation, notify_withdraw, notify_empty, notify_wired " +
            "FROM wired_chests WHERE furni_id = ?)",
        "INSERT INTO wired_contracts (furni_id, payment_mode, receive_text, layout, reward_category, " +
            "show_reward_popup, reward_text) (SELECT ?, payment_mode, receive_text, layout, reward_category, " +
            "show_reward_popup, reward_text FROM wired_contracts WHERE furni_id = ?)",
        "INSERT INTO wired_contract_terms (contract_id, side, rule, position, is_credits, amount, definition_id, " +
            "unknown_sprite_id, poster_code) (SELECT ?, side, rule, position, is_credits, amount, definition_id, " +
            "unknown_sprite_id, poster_code " +
            "FROM wired_contract_terms WHERE contract_id = ?)",
    )

private const val REPOINT_PICKS_SQL =
    "UPDATE wired_box_picks SET furni_id = ? WHERE furni_id = ? AND box_id IN (SELECT id FROM furni WHERE room_id = ?)"

private const val COPY_LAYOUT_SQL =
    "INSERT INTO room_floor_plans (room_id, name, door_x, door_y, door_direction, heightmap) " +
        "(SELECT ?, ?, door_x, door_y, door_direction, heightmap FROM room_floor_plans WHERE room_id = ? LIMIT 1)"

private const val COPY_BOTS_SQL =
    "INSERT INTO bots (player_id, room_id, name, motto, figure, gender, x, y, z, chat_auto, chat_random, " +
        "chat_delay_seconds, dance, kind) (SELECT ?, ?, name, motto, figure, gender, x, y, z, chat_auto, " +
        "chat_random, chat_delay_seconds, dance, kind FROM bots WHERE id = ?)"

private const val TEMPLATE_BOTS_SQL = "SELECT id FROM bots WHERE room_id = ? ORDER BY id"

open class RoomBundles(
    private val services: CatalogServices,
) {
    private val listings get() = services.game.roomListings

    open fun copyFor(
        page: CatalogPage,
        buyer: Player,
    ): Boolean {
        val roomId = page.info.templateRoomId
        val template = listings.room(roomId) ?: return false
        services.game.rooms.saveFurni(roomId)

        val copyId = copyInTransaction(template.id, buyer.profile.id)
        if (copyId == 0) return false

        @Suppress("TooGenericExceptionCaught") // a failure after the commit must never fail the purchase
        try {
            announceRoom(page, template, buyer, copyId)
        } catch (e: RuntimeException) {
            LOGGER.error("room bundle {}: room {} is copied, the rest of the purchase failed", page.id, copyId, e)
        }
        return true
    }

    private fun copyInTransaction(
        templateId: Int,
        userId: Int,
    ): Int =
        LOGGER.orFallback("copying room bundle room $templateId for player $userId", 0) {
            services.database.dataSource.connection.use { connection ->
                connection.allOrNothing { copyRows(connection, templateId, userId).takeIf { it != 0 } } ?: 0
            }
        }

    private fun copyRows(
        connection: Connection,
        templateId: Int,
        userId: Int,
    ): Int {
        val copyId = insertRoom(connection, templateId, userId)
        if (copyId == 0) return 0
        copyDimmer(connection, templateId, copyId)
        copyItems(connection, templateId, copyId, userId)
        copyCustomLayout(connection, templateId, copyId)
        if (services.config.getBoolean("catalog.room_bundles.include_bots")) {
            copyBots(connection, templateId, copyId, userId)
        }
        return copyId
    }

    private fun insertRoom(
        connection: Connection,
        templateId: Int,
        userId: Int,
    ): Int =
        connection.insertForId(COPY_ROOM_SQL) {
            int(userId)
            boolean(rules.newRoomKickSeconds > 0)
            int(rules.newRoomKickSeconds)
            int(templateId)
        } ?: 0

    private fun copyDimmer(
        connection: Connection,
        templateId: Int,
        copyId: Int,
    ) {
        connection.write(COPY_DIMMER_SQL) {
            int(copyId)
            int(templateId)
        }
    }

    private fun copyItems(
        connection: Connection,
        templateId: Int,
        copyId: Int,
        userId: Int,
    ) {
        val template = connection.rows(TEMPLATE_ITEMS_SQL, { int(templateId) }) { it.getInt(1) }
        val copies =
            template.associateWith { furni ->
                connection.insertForId(COPY_ITEM_SQL) {
                    int(userId)
                    int(copyId)
                    int(furni)
                } ?: error("no id for the copy of furni $furni")
            }
        // one statement per row: the driver's bulk batches do not take INSERT ... SELECT
        for (sql in COPY_WIRED_SQL) {
            copies.forEach { (furni, copy) ->
                connection.write(sql) {
                    int(copy)
                    int(furni)
                }
            }
        }
        copies.forEach { (furni, copy) ->
            connection.write(REPOINT_PICKS_SQL) {
                int(copy)
                int(furni)
                int(copyId)
            }
        }
    }

    private fun copyCustomLayout(
        connection: Connection,
        templateId: Int,
        copyId: Int,
    ) {
        connection.write(COPY_LAYOUT_SQL) {
            int(copyId)
            string("custom_$copyId")
            int(templateId)
        }
    }

    private fun copyBots(
        connection: Connection,
        templateId: Int,
        copyId: Int,
        userId: Int,
    ) {
        val templateBots = connection.rows(TEMPLATE_BOTS_SQL, { int(templateId) }) { it.getInt(1) }
        for (templateBot in templateBots) {
            val copy =
                connection.insertForId(COPY_BOTS_SQL) {
                    int(userId)
                    int(copyId)
                    int(templateBot)
                } ?: continue
            BotChatLines.copyLines(connection, from = templateBot, to = copy)
        }
    }

    private fun announceRoom(
        page: CatalogPage,
        template: ListedRoom,
        buyer: Player,
        copyId: Int,
    ) {
        val keys =
            hashMapOf(
                "ROOMNAME" to template.name,
                "ROOMID" to copyId.toString(),
                "OWNER" to buyer.profile.name,
                "image" to "\${image.library.url}/notifications/room_bundle_${page.id}.png",
            )
        buyer.client?.send(BubbleAlertMessage(BubbleAlert.ROOM_BOUGHT.key, keys))
        giveBundleBadge(page, buyer)
    }

    private fun giveBundleBadge(
        page: CatalogPage,
        player: Player,
    ) {
        val offer = page.offers.values.firstOrNull() ?: return
        val badges = player.inventory.badges
        for (badgeItem in offer.products) {
            if (badgeItem.type != ProductKind.BADGE || badges.owns(badgeItem.name)) continue
            badges.award(badgeItem.name, asUnseen = false)
        }
    }
}
