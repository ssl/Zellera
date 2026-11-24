package zel.protocol.outgoing.users

import zel.db.orFallback
import zel.game.guilds.Guild
import zel.game.players.Player
import zel.game.players.PlayerProfile
import zel.game.players.inventory.BadgeLeaderboard
import zel.game.players.inventory.BadgeTier
import zel.game.players.level.AccountLevelTable
import zel.protocol.generated.ExtendedProfileOut
import zel.protocol.generated.ExtendedProfileOutCodec
import zel.protocol.outgoing.OutgoingMessage
import zel.util.GameZone
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.time.Instant
import java.time.format.DateTimeFormatter

/** A hidden profile shows others only name, look, motto and level. */
class UserProfile private constructor(
    val profile: PlayerProfile,
    val player: Player?,
    val viewer: Player,
    private val openWindow: Boolean,
) : OutgoingMessage<ExtendedProfileOut>(ExtendedProfileOutCodec) {
    constructor(profile: PlayerProfile, viewer: Player, openWindow: Boolean = true) :
        this(profile, null, viewer, openWindow)

    constructor(player: Player, viewer: Player, openWindow: Boolean = true) :
        this(player.profile, player, viewer, openWindow)

    private val services get() = viewer.services
    private val messenger get() = services.game.messengerModule

    override fun message(): ExtendedProfileOut {
        val id = profile.id
        val (achievementScore, hidden) = scoreAndVisibility(profile)
        val concealed = hidden && viewer.profile.id != id
        val boards = services.badgeCaches.leaderboard
        val totals = boards.board(BadgeLeaderboard.TYPE_TOTAL_BADGES, -1)
        val levels = boards.board(BadgeLeaderboard.TYPE_ACHIEVEMENT_LEVEL, -1)
        val level = boards.accountStanding(id).let { AccountLevelTable.kept(it.level, it.countedPoints) }
        return ExtendedProfileOut(
            int1 = id,
            string1 = profile.name,
            string2 = profile.figure,
            string3 = profile.motto,
            string4 = CREATED_FORMAT.format(Instant.ofEpochSecond(profile.createdAt.toLong())),
            int2 = achievementScore,
            int3 = if (concealed) CONCEALED else messenger?.friends?.countOf(id) ?: 0,
            boolean1 = viewer.messenger.friends.containsKey(id),
            boolean2 = messenger?.requests?.isWaiting(id, viewer.profile.id) ?: false,
            byte1 = (if (concealed) STATUS_HIDDEN else onlineStatus(profile)).toByte(),
            list1 = if (concealed) emptyList() else guildsOf(profile).map { guildEntry(it, id) },
            int4 = if (concealed) CONCEALED else services.unixTimestamp() - profile.lastOnlineAt,
            boolean3 = openWindow,
            boolean4 = hidden,
            int5 = level.level,
            int6 = if (concealed) CONCEALED else level.nextStart,
            int7 = if (concealed) CONCEALED else 0,
            boolean5 = true,
            boolean6 = false,
            int8 = totals?.scoreOf(id) ?: storedBadgeCount(id),
            int9 = levels?.scoreOf(id) ?: 0,
            list2 = rarityScores(boards, id),
            int10 = totals?.rankOf(id) ?: -1,
        )
    }

    private fun rarityScores(
        boards: BadgeLeaderboard,
        id: Int,
    ): List<ExtendedProfileOut.List2Item> =
        BadgeTier.codes.mapNotNull { rarity ->
            val score = boards.board(BadgeLeaderboard.TYPE_RARITY, rarity)?.scoreOf(id) ?: 0
            if (score > 0) ExtendedProfileOut.List2Item(rarity.toByte(), score) else null
        }

    private fun guildEntry(
        guild: Guild,
        id: Int,
    ): ExtendedProfileOut.List1Item {
        val parts = checkNotNull(services.game.guilds).parts
        return ExtendedProfileOut.List1Item(
            guild.id,
            guild.name,
            guild.badge,
            parts.symbolColourHex(guild),
            parts.backgroundColourHex(guild),
            player != null && guild.id == player.stats.favouriteGuild,
            guild.ownerId,
            guild.ownerId == id,
        )
    }

    private fun scoreAndVisibility(profile: PlayerProfile): Pair<Int, Boolean> {
        player?.stats?.let { return it.tally.achievementScore to it.settings.privateProfile }
        return stored(SETTINGS_SQL, profile.id) { it.getInt("achievement_score") to it.getBoolean("hide_profile") }
            ?: (0 to false)
    }

    private fun guildsOf(profile: PlayerProfile): List<Guild> {
        val directory = checkNotNull(services.game.guilds)
        val stats = player?.stats ?: return directory.ofMember(profile.id)
        val ids = synchronized(stats) { stats.guildIds.toList() }
        return ids.asReversed().filter { it != 0 }.mapNotNull(directory::byId)
    }

    private fun onlineStatus(profile: PlayerProfile): Int =
        when {
            !profile.isOnline -> STATUS_OFFLINE
            player?.stats?.settings?.appearOffline != true -> STATUS_ONLINE
            viewer.profile.id == profile.id -> STATUS_HIDDEN
            else -> STATUS_OFFLINE
        }

    private fun storedBadgeCount(id: Int): Int = stored(BADGE_COUNT_SQL, id) { it.getInt("total_badges") } ?: 0

    private fun <T : Any> stored(
        sql: String,
        id: Int,
        read: (ResultSet) -> T,
    ): T? =
        log.orFallback("reading profile data of player $id", null) {
            services.database.queries
                .select(sql, id)
                .first(read)
        }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(UserProfile::class.java)
        val CREATED_FORMAT: DateTimeFormatter get() =
            DateTimeFormatter
                .ofPattern(
                    "dd-MM-yyyy",
                ).withZone(GameZone.current)
        const val CONCEALED = -1
        const val STATUS_OFFLINE = 0
        const val STATUS_ONLINE = 1
        const val STATUS_HIDDEN = 2
        const val SETTINGS_SQL =
            "SELECT s.achievement_score, pr.hide_profile FROM player_stats s " +
                "INNER JOIN player_preferences pr ON pr.player_id = s.player_id WHERE s.player_id = ? LIMIT 1"
        const val BADGE_COUNT_SQL =
            "SELECT COUNT(DISTINCT badge_code) AS total_badges FROM player_badges WHERE player_id = ?"
    }
}
