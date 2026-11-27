package zel.game.webcommands

import zel.db.write
import java.sql.SQLException

/** Website privacy settings; an online player's settings change in memory too, so their next save keeps them. */
internal class PrivacyWebCommands(
    private val services: WebCommandApplier.Services,
) {
    /** A payload key (true allows) and its `player_preferences` column (set forbids). */
    internal enum class Choice(
        val key: String,
        val column: String,
    ) {
        PROFILE_VISIBLE("profileVisible", "hide_profile"),
        ONLINE_STATUS_VISIBLE("onlineStatusVisible", "hide_online"),
        FRIEND_CAN_FOLLOW("friendCanFollow", "block_following"),
        FRIEND_REQUEST_ENABLED("friendRequestEnabled", "block_friend_requests"),
        ROOM_INVITES_ENABLED("roomInvitesEnabled", "block_room_invites"),
    }

    @Suppress("ReturnCount") // one early answer per refusal
    @Throws(SQLException::class)
    fun set(
        command: WebCommand,
        payload: WebCommandPayload,
    ): WebCommandResult {
        val given = Choice.entries.mapNotNull { choice -> payload.bool(choice.key)?.let { choice to !it } }
        if (given.isEmpty()) return WebCommandResult.failed("The payload names no privacy choice.")

        services.dataSource.connection.use { connection ->
            if (!WebCommandApplier.userExists(connection, command.userId)) {
                return WebCommandResult.failed("No user has id ${command.userId}.")
            }
            val columns = given.joinToString(", ") { "`${it.first.column}`" }
            val marks = given.joinToString(", ") { "?" }
            val updates = given.joinToString(", ") { "`${it.first.column}` = VALUES(`${it.first.column}`)" }
            connection.write(
                "INSERT INTO player_preferences (player_id, $columns) VALUES (?, $marks) " +
                    "ON DUPLICATE KEY UPDATE $updates",
            ) {
                int(command.userId)
                given.forEach { boolean(it.second) }
            }
        }
        services.online(command.userId)?.let { player ->
            val settings = player.stats.settings
            val wasOffline = settings.appearOffline
            given.forEach { (choice, stored) ->
                when (choice) {
                    Choice.PROFILE_VISIBLE -> settings.privateProfile = stored
                    Choice.ONLINE_STATUS_VISIBLE -> settings.appearOffline = stored
                    Choice.FRIEND_CAN_FOLLOW -> settings.noFollowing = stored
                    Choice.FRIEND_REQUEST_ENABLED -> settings.noFriendRequests = stored
                    Choice.ROOM_INVITES_ENABLED -> settings.noRoomInvites = stored
                }
            }
            if (settings.appearOffline != wasOffline) {
                player.messenger.connectionChanged(player, player.isOnline, player.inRoom)
            }
        }
        return WebCommandResult.done("Privacy saved: ${given.joinToString { it.first.key }}.")
    }
}
