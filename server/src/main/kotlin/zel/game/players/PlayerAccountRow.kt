package zel.game.players

import zel.game.permissions.Rank
import zel.game.permissions.SecurityLevel
import zel.server.GameServices
import java.sql.ResultSet

/** A player without an account row (content accounts) reads with empty account fields. */
internal data class PlayerAccountRow(
    val id: Int,
    val name: String = "",
    val motto: String = "",
    val figure: String = "",
    val gender: Gender = Gender.M,
    val lastIp: String = "",
    val rank: Rank = Rank(SecurityLevel.NONE),
    val createdAt: Int = 0,
    val credits: Int = 0,
    val lastOnlineAt: Int = 0,
    val homeRoomId: Int = 0,
    val machineId: String = "",
) {
    companion object {
        /** Append a `WHERE`. */
        const val SELECT =
            "SELECT p.*, a.last_ip, a.machine_id, " +
                "COALESCE((SELECT b.amount FROM player_balances b WHERE b.player_id = p.id AND b.currency = -1), 0) " +
                "AS credits FROM players p LEFT JOIN player_accounts a ON a.id = p.account_id"

        fun read(
            services: GameServices,
            set: ResultSet,
        ): PlayerAccountRow {
            val rankId = set.getInt("rank_id")
            val rank =
                services.game.permissions
                    ?.ranks
                    ?.get(rankId) ?: Rank(rankId)
            return PlayerAccountRow(
                id = set.getInt("id"),
                name = set.getString("name"),
                motto = set.getString("motto").orEmpty(),
                figure = set.getString("figure"),
                gender = Gender.ofCode(set.getString("gender")) ?: Gender.M,
                lastIp = set.getString("last_ip").orEmpty(),
                rank = rank,
                createdAt = set.getInt("created_at"),
                credits = set.getInt("credits"),
                lastOnlineAt = set.getInt("last_online_at"),
                homeRoomId = set.getInt("home_room_id"),
                machineId = set.getString("machine_id").orEmpty(),
            )
        }
    }
}
