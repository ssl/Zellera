package zel.game.players

import org.slf4j.LoggerFactory
import zel.db.SqlFailure
import zel.db.noneAsNull
import zel.server.GameServices

private val log = LoggerFactory.getLogger(PlayerStatsStore::class.java)

private const val SAVE_SQL =
    "UPDATE player_stats s INNER JOIN player_preferences pr ON pr.player_id = s.player_id " +
        "INNER JOIN player_standing st ON st.player_id = s.player_id " +
        "SET s.achievement_score = GREATEST(s.achievement_score, ?), s.respect_received = ?, " +
        "s.respect_given = ?, s.daily_respect_left = ?, " +
        "s.online_seconds = s.online_seconds + ?, s.favourite_guild_id = ?, s.daily_pet_respect_left = ?, " +
        "s.login_streak = ?, s.rented_space_furni_id = ?, s.rented_space_ends_at = ?, pr.volume_system = ?, " +
        "pr.volume_furni = ?, pr.volume_music = ?, pr.old_chat = ?, " +
        "pr.block_camera_follow = ?, pr.chat_color = ?, s.hall_of_fame_points = ?, pr.ignore_bots = ?, " +
        "pr.ignore_pets = ?, st.welcome_gift_taken = ?, st.muted_until = ?, st.may_change_name = ?, " +
        "st.can_trade = ?, " +
        "s.forum_post_count = ?, pr.ui_flags = ?, pr.saved_searches_seeded = ?, st.max_friends = ?, " +
        "st.max_rooms = ?, s.last_club_payday_at = ?, s.club_gifts_claimed = ?, s.build_pass_bonus_furni = ?, " +
        "s.marketplace_tokens = ?, s.new_additions_seen = ? WHERE s.player_id = ?"

private const val SAVE_OFFER_PURCHASE =
    "UPDATE player_target_offers SET state = ?, bought = ?, last_bought_at = ? WHERE player_id = ? AND target_offer_id = ?"

private const val SAVE_DISCORD_SQL =
    "UPDATE player_preferences SET discord_version = ?, discord_sharing = ? WHERE player_id = ?"

private const val WRITE_MUTE_SQL = "UPDATE player_standing SET muted_until = ? WHERE player_id = ?"

private const val READ_MUTE_SQL =
    "SELECT COALESCE(muted_until, 0) AS muted_until FROM player_standing WHERE player_id = ?"

private const val READ_RESPECTS_SQL =
    "SELECT daily_respect_left, daily_pet_respect_left, daily_respect_refills_left FROM player_stats WHERE player_id = ?"

internal data class RespectCounters(
    val respectsLeft: Int,
    val petRespectsLeft: Int,
    val refillsLeft: Int,
)

internal class PlayerStatsStore(
    private val services: GameServices,
    private val playerId: Int,
) {
    @Volatile
    private var closed = false

    fun close() {
        closed = true
    }

    fun requireOpen() = check(!closed) { "the stats are disposed" }

    @Throws(SqlFailure::class)
    fun saveSnapshot(
        stats: PlayerStats,
        onlineSeconds: Int,
    ) {
        requireOpen()
        val tally = stats.tally
        val settings = stats.settings
        val standing = stats.standing
        val club = stats.club
        services.database.queries.write(
            SAVE_SQL,
            tally.achievementScore,
            tally.respectsReceived,
            tally.respectsGiven,
            tally.respectsLeft,
            onlineSeconds,
            noneAsNull(stats.favouriteGuild),
            tally.petRespectsLeft,
            tally.loginStreak,
            noneAsNull(stats.rentedSpaceId),
            noneAsNull(stats.rentedUntil),
            settings.volumes.system,
            settings.volumes.furni,
            settings.volumes.trax,
            settings.classicChat,
            settings.noCameraFollow,
            settings.bubble.type,
            tally.hallOfFamePoints,
            settings.mutedBots,
            settings.mutedPets,
            standing.welcomeGiftTaken,
            noneAsNull(standing.mutedUntil),
            standing.mayChangeName,
            standing.tradePass,
            tally.forumPosts,
            settings.clientFlags,
            settings.starterSearchesGiven,
            standing.maxFriends,
            standing.maxRooms,
            noneAsNull(club.lastPayday),
            club.giftsClaimed,
            club.buildPassBonusFurni,
            tally.marketplaceTokens,
            tally.newAdditionsSeen,
            playerId,
        )
    }

    fun storeMutedUntil(until: Int) =
        services.threading.write(MuteLane(playerId)) {
            try {
                services.database.queries.write(WRITE_MUTE_SQL, until, playerId)
            } catch (e: SqlFailure) {
                log.error("Storing the mute of user {} failed", playerId, e)
            }
        }

    private data class MuteLane(
        val playerId: Int,
    )

    fun readMutedUntil(): Int? =
        try {
            services.database.queries
                .select(READ_MUTE_SQL, playerId)
                .first { it.getInt("muted_until") }
        } catch (e: SqlFailure) {
            log.error("Reading the current mute of user {} failed", playerId, e)
            null
        }

    fun readRespectRefill(): RespectCounters? =
        try {
            services.database.queries.select(READ_RESPECTS_SQL, playerId).first {
                RespectCounters(
                    it.getInt("daily_respect_left"),
                    it.getInt("daily_pet_respect_left"),
                    it.getInt("daily_respect_refills_left"),
                )
            }
        } catch (e: SqlFailure) {
            log.error("Reading the daily respects of user {} failed", playerId, e)
            null
        }

    @Throws(SqlFailure::class)
    fun startOfferPurchase(offerId: Int) {
        requireOpen()
        services.database.queries.write(
            "INSERT INTO player_target_offers (player_id, target_offer_id) VALUES (?, ?)",
            playerId,
            offerId,
        )
    }

    @Throws(SqlFailure::class)
    fun saveOfferPurchases(purchases: Collection<PlayerOfferPurchase>) {
        requireOpen()
        val changed = purchases.filter { it.changed }
        if (changed.isEmpty()) return
        services.database.queries.writeEach(SAVE_OFFER_PURCHASE, changed) { purchase ->
            int(purchase.state)
            int(purchase.bought)
            intOrNull(purchase.lastBoughtAt)
            int(playerId)
            int(purchase.offerId)
        }
        changed.forEach(PlayerOfferPurchase::markSaved)
    }

    fun saveDiscord(value: DiscordPreferences) =
        write(
            "discord settings",
            SAVE_DISCORD_SQL,
            value.version,
            value.sharing,
            playerId,
        )

    fun addFavouriteRoom(roomId: Int) =
        write(
            "favourite room",
            "INSERT INTO player_favourite_rooms (player_id, room_id) VALUES (?, ?)",
            playerId,
            roomId,
        )

    fun removeFavouriteRoom(roomId: Int) =
        write(
            "favourite room",
            "DELETE FROM player_favourite_rooms WHERE player_id = ? AND room_id = ? LIMIT 1",
            playerId,
            roomId,
        )

    fun addRecipe(recipeId: Int) =
        write("recipe", "INSERT INTO player_recipes (player_id, recipe_id) VALUES (?, ?)", playerId, recipeId)

    fun addIgnore(targetId: Int) =
        write("ignore", "INSERT IGNORE INTO player_ignores (player_id, ignored_id) VALUES (?, ?)", playerId, targetId)

    fun removeIgnore(targetId: Int) =
        write("ignore", "DELETE FROM player_ignores WHERE player_id = ? AND ignored_id = ?", playerId, targetId)

    fun addBlock(targetId: Int) =
        write("block", "INSERT IGNORE INTO player_blocks (player_id, blocked_id) VALUES (?, ?)", playerId, targetId)

    fun removeBlock(targetId: Int) =
        write("block", "DELETE FROM player_blocks WHERE player_id = ? AND blocked_id = ?", playerId, targetId)

    fun addFilterWord(word: String?) =
        write("word filter", "INSERT IGNORE INTO player_word_filters (player_id, word) VALUES (?, ?)", playerId, word)

    fun removeFilterWord(word: String?) =
        write("word filter", "DELETE FROM player_word_filters WHERE player_id = ? AND word = ?", playerId, word)

    fun saveRespectRefill(
        refillsLeft: Int,
        respectLeft: Int,
    ) = write(
        "respect replenish",
        "UPDATE player_stats SET daily_respect_refills_left = ?, daily_respect_left = ? WHERE player_id = ? LIMIT 1",
        refillsLeft,
        respectLeft,
        playerId,
    )

    fun saveChatLayout(layout: PlayerSettings.ChatLayout) =
        write(
            "chat preferences",
            "UPDATE player_preferences SET chat_mode = ?, chat_bubble_width = ?, chat_scroll_speed = ? " +
                "WHERE player_id = ? LIMIT 1",
            layout.mode,
            layout.bubbleWidth,
            layout.scrollSpeed,
            playerId,
        )

    fun savePreferences(settings: PlayerSettings) =
        write(
            "preferences",
            "UPDATE player_preferences SET volume_system = ?, volume_furni = ?, volume_music = ?, ui_flags = ?, " +
                "chat_color = ?, block_room_invites = ?, block_camera_follow = ? WHERE player_id = ? LIMIT 1",
            settings.volumes.system,
            settings.volumes.furni,
            settings.volumes.trax,
            settings.clientFlags,
            settings.bubble.type,
            settings.noRoomInvites,
            settings.noCameraFollow,
            playerId,
        )

    fun saveOnlineIndicator(preference: Int) =
        write(
            "online_indicator_preference",
            "UPDATE player_preferences SET `online_indicator` = ? WHERE player_id = ? LIMIT 1",
            preference,
            playerId,
        )

    fun saveWiredFlag(
        flag: PlayerSettings.WiredColumn,
        enabled: Boolean,
    ) = write(
        flag.column,
        "UPDATE player_preferences SET `${flag.column}` = ? WHERE player_id = ? LIMIT 1",
        enabled,
        playerId,
    )

    fun saveModeratorWindow(window: PlayerSettings.ModeratorWindow) =
        write(
            "mod tool window",
            "UPDATE player_preferences SET mod_window_x = ?, mod_window_y = ?, mod_window_width = ?, " +
                "mod_window_height = ? WHERE player_id = ? LIMIT 1",
            window.x,
            window.y,
            window.width,
            window.height,
            playerId,
        )

    fun saveNavigatorWindow(window: PlayerSettings.NavigatorWindow) =
        write(
            "navigator window",
            "UPDATE player_preferences SET nav_window_x = ?, nav_window_y = ?, nav_window_width = ?, " +
                "nav_window_height = ?, nav_window_searches_open = ?, nav_window_results_mode = ? " +
                "WHERE player_id = ? LIMIT 1",
            window.x,
            window.y,
            window.width,
            window.height,
            window.searchesOpen,
            window.resultsMode,
            playerId,
        )

    private fun write(
        what: String,
        sql: String,
        vararg params: Any?,
    ) {
        requireOpen()
        try {
            services.database.queries.write(sql, *params)
        } catch (e: SqlFailure) {
            log.error("Failed to persist {} for user {}", what, playerId, e)
        }
    }
}
