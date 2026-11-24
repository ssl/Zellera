package zel.game.guilds

import java.sql.ResultSet

data class Membership(
    val playerId: Int,
    val name: String,
    val look: String,
    val since: Int,
    val role: MemberLevel,
) {
    val standing: GuildStanding get() = role.standing

    companion object {
        fun of(row: ResultSet): Membership =
            Membership(
                playerId = row.getInt("player_id"),
                name = row.getString("name").orEmpty(),
                look = row.getString("figure").orEmpty(),
                since = row.getInt("member_since"),
                role = MemberLevel.fromCode(row.getInt("level")),
            )
    }
}
