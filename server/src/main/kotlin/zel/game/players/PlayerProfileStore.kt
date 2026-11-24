package zel.game.players

import zel.db.SqlFailure
import zel.db.noneAsNull
import zel.db.orFallback
import zel.game.messenger.FriendCategory
import zel.game.navigation.NavigatorSavedSearch
import zel.server.GameServices
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(PlayerProfileStore::class.java)

private const val SAVE_SQL =
    "UPDATE players p JOIN player_accounts a ON a.id = p.account_id SET p.is_online = ?, p.figure = ?, p.gender = ?, " +
        "p.last_login_at = ?, p.last_online_at = ?, p.home_room_id = (SELECT id FROM rooms WHERE id = ?), " +
        "a.last_ip = ?, a.machine_id = ?, p.name = ? " +
        "WHERE p.id = ?"

internal class PlayerProfileStore(
    private val services: GameServices,
    private val playerId: Int,
) {
    fun save(profile: PlayerProfile) {
        try {
            services.database.queries.write(
                SAVE_SQL,
                profile.isOnline,
                profile.figure,
                profile.gender.name,
                services.unixTimestamp(),
                noneAsNull(profile.lastOnlineAt),
                noneAsNull(profile.homeRoomId),
                profile.lastIp,
                profile.machineId,
                profile.name,
                playerId,
            )
        } catch (e: SqlFailure) {
            log.error("Saving the profile of player {} failed", playerId, e)
        }
    }

    fun readBalances(into: WalletBalances) =
        log.orFallback("Reading the balances of player $playerId", Unit) {
            services.database.queries
                .select(
                    "SELECT currency, amount FROM player_balances WHERE player_id = ? AND currency >= 0",
                    playerId,
                ).each {
                    into.restoreCurrency(it.getInt("currency"), it.getInt("amount"))
                }
        }

    fun readSavedSearches(): List<NavigatorSavedSearch> =
        log.orFallback("Reading the saved searches of player $playerId", emptyList()) {
            services.database.queries
                .select(
                    "SELECT number, view_code, search_text FROM player_saved_searches " +
                        "WHERE player_id = ? ORDER BY number",
                    playerId,
                ).rows {
                    NavigatorSavedSearch(it.getString("view_code"), it.getString("search_text"), it.getInt("number"))
                }
        }

    fun readFriendCategories(): List<FriendCategory> =
        log.orFallback("Reading the friend categories of player $playerId", emptyList()) {
            services.database.queries
                .select(
                    "SELECT id, player_id, name FROM friend_categories WHERE player_id = ?",
                    playerId,
                ).rows {
                    FriendCategory(it.getInt("id"), it.getInt("player_id"), it.getString("name"))
                }
        }

    /** False when the number is taken or the player saved the same search before. */
    fun addSavedSearch(search: NavigatorSavedSearch): Boolean =
        log.orFallback("Saving a search of player $playerId", false) {
            services.database.queries.write(
                "INSERT IGNORE INTO player_saved_searches (player_id, number, view_code, search_text) " +
                    "VALUES (?, ?, ?, ?)",
                playerId,
                search.id,
                search.searchCode.orEmpty(),
                search.filter.orEmpty(),
            ) > 0
        }

    fun deleteSavedSearch(number: Int) {
        try {
            services.database.queries.write(
                "DELETE FROM player_saved_searches WHERE player_id = ? AND number = ?",
                playerId,
                number,
            )
        } catch (e: SqlFailure) {
            log.error("Deleting saved search {} of player {} failed", number, playerId, e)
        }
    }
}
