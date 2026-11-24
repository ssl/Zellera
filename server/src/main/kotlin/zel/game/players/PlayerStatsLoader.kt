package zel.game.players

import zel.db.SqlFailure
import zel.db.first
import zel.db.rows
import zel.server.GameServices
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

private val log = LoggerFactory.getLogger(PlayerStatsLoader::class.java)

private const val SETTINGS_ROW =
    "SELECT * FROM player_stats s INNER JOIN player_preferences pr ON pr.player_id = s.player_id " +
        "INNER JOIN player_standing st ON st.player_id = s.player_id WHERE s.player_id = ? LIMIT 1"

private const val ACHIEVEMENT_PROGRESS =
    "SELECT a.code, p.progress FROM player_achievements p " +
        "JOIN achievements a ON a.id = p.achievement_id WHERE p.player_id = ?"

internal class PlayerStatsLoader(
    private val services: GameServices,
    private val profile: PlayerProfile,
) {
    private val playerId = profile.id

    fun load(): PlayerStats {
        read()?.let { return it }
        profile.firstVisit = true
        createRows()
        return checkNotNull(read()) { "The settings rows of user $playerId could not be created" }
    }

    private fun read(): PlayerStats? =
        try {
            services.database.dataSource.connection.use { connection ->
                connection.readSettings()?.also { connection.readLists(it) }
            }
        } catch (e: SQLException) {
            throw IllegalStateException("The settings of user $playerId could not be loaded", e)
        }

    private fun createRows() {
        try {
            val queries = services.database.queries
            queries.write("INSERT IGNORE INTO player_stats (player_id) VALUES (?)", playerId)
            queries.write("INSERT IGNORE INTO player_preferences (player_id) VALUES (?)", playerId)
            queries.write("INSERT IGNORE INTO player_standing (player_id) VALUES (?)", playerId)
        } catch (e: SqlFailure) {
            log.error("Creating the stats rows of player {} failed", playerId, e)
        }
    }

    private fun Connection.readSettings(): PlayerStats? =
        first(SETTINGS_ROW, { int(playerId) }) { row -> PlayerStats(services, profile).also { fill(it, row) } }

    private fun fill(
        stats: PlayerStats,
        row: ResultSet,
    ) {
        stats.tally.read(row)
        stats.settings.load(row, services.chatBubbles::of)
        stats.standing.read(row)
        stats.club.read(row)
        stats.favouriteGuild = row.getInt("favourite_guild_id")
        stats.rentedSpaceId = row.getInt("rented_space_furni_id")
        stats.rentedUntil = row.getInt("rented_space_ends_at")
        stats.club.subscriptions =
            services.game.subscriptions
                .required()
                .ofPlayer(playerId)
    }

    private fun Connection.readLists(stats: PlayerStats) {
        each("SELECT guild_id FROM guild_members WHERE player_id = ? AND level < 3 LIMIT 100") {
            stats.guildIds.add(it.getInt("guild_id"))
        }
        stats.guildIds.sort()
        each(ACHIEVEMENT_PROGRESS) { row ->
            services.game.achievements.required().named(row.getString("code"))?.let {
                stats.achievementProgress[it] = row.getInt("progress")
            }
        }
        each("SELECT section, list_mode, collapsed FROM player_navigator_sections WHERE player_id = ?") {
            stats.navigatorLayouts.fromRow(
                it.getString("section"),
                it.getString("list_mode"),
                it.getBoolean("collapsed"),
            )
        }
        each("SELECT room_id FROM player_favourite_rooms WHERE player_id = ?") {
            stats.favouriteRooms.restore(it.getInt("room_id"))
        }
        each("SELECT * FROM player_recipes WHERE player_id = ?") { stats.recipes.restore(it.getInt("recipe_id")) }
        each(
            "SELECT offer_id, sold_at FROM catalogue_limited_sales WHERE player_id = ? AND sold_at > ?",
            services.unixTimestamp() - LimitedPurchaseLog.WINDOW_SECONDS,
        ) { stats.limitedPurchases.add(it.getInt("offer_id"), it.getInt("sold_at")) }
        each("SELECT * FROM player_target_offers WHERE player_id = ?") {
            val offerId = it.getInt("target_offer_id")
            stats.offerPurchases[offerId] =
                PlayerOfferPurchase(offerId, it.getInt("state"), it.getInt("bought"), it.getInt("last_bought_at"))
        }
        readContacts(stats)
    }

    private fun Connection.readContacts(stats: PlayerStats) {
        each("SELECT ignored_id FROM player_ignores WHERE player_id = ?") {
            stats.contacts.restoreIgnored(it.getInt(1))
        }
        each("SELECT blocked_id FROM player_blocks WHERE player_id = ?") {
            stats.contacts.restoreBlocked(it.getInt(1))
        }
        each("SELECT word FROM player_word_filters WHERE player_id = ? ORDER BY word COLLATE utf8mb4_general_ci") {
            stats.contacts.customFilter.add(it.getString(1))
        }
    }

    private fun Connection.each(
        sql: String,
        vararg extra: Any?,
        body: (ResultSet) -> Unit,
    ) {
        rows(sql, {
            int(playerId)
            extra.forEach { value(it) }
        }, body)
    }
}
