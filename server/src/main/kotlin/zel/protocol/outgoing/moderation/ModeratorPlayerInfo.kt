package zel.protocol.outgoing.moderation

import zel.db.Sql
import zel.db.SqlFailure
import zel.game.moderation.Sanctions
import zel.protocol.generated.ModeratorPlayerInfoOut
import zel.util.GameZone
import zel.util.SECONDS_PER_HOUR
import zel.util.SECONDS_PER_MINUTE
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

class ModeratorPlayerInfo(
    dataSource: DataSource,
    private val sanctions: Sanctions,
    private val clock: () -> Int,
) {
    private val queries = Sql(dataSource)

    /** How much of a player's sheet a moderator gets. */
    enum class View {
        /** A player the moderator does not reach: who they are and whether they are online, nothing kept on them. */
        CARD,

        /** The standing of a player the moderator reaches. */
        RECORD,

        /** The record with the account's e-mail, for those who also hold the ban right. */
        RECORD_WITH_IDENTITY,
    }

    fun read(
        playerId: Int,
        view: View,
    ): ModeratorPlayerInfoOut? =
        try {
            if (view == View.CARD) {
                queries.select(CARD_SQL, playerId).first(::card)
            } else {
                queries.select(SHEET_SQL, playerId).first { sheet(it, view) }
            }
        } catch (e: SqlFailure) {
            log.error("Moderator info of player {} not read", playerId, e)
            null
        }

    private fun card(row: ResultSet): ModeratorPlayerInfoOut {
        val playerId = row.getInt("id")
        val now = clock()
        val online = row.getBoolean("is_online")
        return ModeratorPlayerInfoOut(
            int1 = playerId,
            string1 = row.getString("name").orEmpty(),
            string2 = row.getString("figure").orEmpty(),
            int2 = (now - row.getInt("created_at")) / SECONDS_PER_MINUTE,
            int3 = if (online) 0 else (now - row.getInt("last_online_at")) / SECONDS_PER_MINUTE,
            boolean1 = online,
            int4 = 0,
            int5 = 0,
            int6 = 0,
            int7 = 0,
            int8 = 0,
            string3 = "",
            string4 = "",
            int9 = playerId,
            int10 = 0,
            string5 = "",
            string6 = "Rank (${row.getInt("rank_id")}): ${row.getString("rank_name")}",
            trailing1 = null,
        )
    }

    private fun sheet(
        row: ResultSet,
        view: View,
    ): ModeratorPlayerInfoOut {
        val playerId = row.getInt("id")
        val now = clock()
        val online = row.getBoolean("is_online")
        val offlineFor = if (online) 0 else now - row.getInt("last_online_at")
        return ModeratorPlayerInfoOut(
            int1 = playerId,
            string1 = row.getString("name").orEmpty(),
            string2 = row.getString("figure").orEmpty(),
            int2 = (now - row.getInt("created_at")) / SECONDS_PER_MINUTE,
            int3 = offlineFor / SECONDS_PER_MINUTE,
            boolean1 = online,
            int4 = row.getInt("reports_sent"),
            int5 = row.getInt("reports_abusive"),
            int6 = row.getInt("warnings_received"),
            int7 = row.getInt("times_banned"),
            int8 = row.getInt("trade_lock_count"),
            string3 = formatUnixTimestamp(activeTradeLockExpiry(row.getInt("trade_locked_until"), now)),
            string4 = formatUnixTimestamp(row.getInt("last_bought_at")),
            int9 = playerId,
            int10 = row.getInt("banned_on_devices"),
            string5 = if (view == View.RECORD_WITH_IDENTITY) row.getString("email").orEmpty() else "",
            string6 = "Rank (${row.getInt("rank_id")}): ${row.getString("rank_name")}",
            trailing1 = lastSanction(playerId, now),
        )
    }

    /** When the newest ladder step was given and how many hours ago, which the tool shows in red for two days. */
    private fun lastSanction(
        playerId: Int,
        now: Int,
    ): ModeratorPlayerInfoOut.Trailing1? {
        val last = sanctions.latestOf(playerId) ?: return null
        val hoursAgo = (now - last.issuedAt) / SECONDS_PER_HOUR
        return ModeratorPlayerInfoOut.Trailing1(formatUnixTimestamp(last.issuedAt), hoursAgo)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ModeratorPlayerInfo::class.java)
        private val DATE_FORMAT: DateTimeFormatter
            get() = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(GameZone.current)
        private const val CARD_SQL =
            "SELECT p.id, p.name, p.figure, p.created_at, p.last_online_at, p.is_online, p.rank_id, " +
                "r.name AS rank_name FROM players p JOIN ranks r ON r.id = p.rank_id WHERE p.id = ?"

        // banned_on_devices: other players who logged in from a device of this one and were banned at some time
        private const val SHEET_SQL =
            "SELECT p.id, p.name, p.figure, p.created_at, p.last_online_at, p.is_online, p.rank_id, " +
                "r.name AS rank_name, a.email, st.reports_sent, st.reports_abusive, st.warnings_received, " +
                "st.trade_lock_count, st.trade_locked_until, " +
                "(SELECT COUNT(*) FROM bans b WHERE b.player_id = p.id) AS times_banned, " +
                "(SELECT MAX(bought.bought_at) FROM purchases bought WHERE bought.player_id = p.id) " +
                "AS last_bought_at, " +
                "(SELECT COUNT(DISTINCT other.player_id) FROM player_devices own " +
                "JOIN player_devices other ON other.id = own.id AND other.player_id <> own.player_id " +
                "WHERE own.player_id = p.id AND own.id NOT IN ('0000000000000000', 'ffffffffffffffff') " +
                "AND EXISTS (SELECT 1 FROM bans seen WHERE seen.player_id = other.player_id)) AS banned_on_devices " +
                "FROM players p JOIN player_accounts a ON a.id = p.account_id " +
                "JOIN player_standing st ON st.player_id = p.id JOIN ranks r ON r.id = p.rank_id WHERE p.id = ?"

        fun activeTradeLockExpiry(
            tradeLockedUntil: Int,
            now: Int,
        ): Int = if (tradeLockedUntil > now) tradeLockedUntil else 0

        fun formatUnixTimestamp(timestamp: Int): String =
            if (timestamp <= 0) "" else DATE_FORMAT.format(Instant.ofEpochSecond(timestamp.toLong()))
    }
}
