package zel.game.webcommands

import org.slf4j.LoggerFactory
import zel.db.first
import zel.game.permissions.Privilege
import zel.game.players.Player
import zel.room.port.RoomPort
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

/** Applies one `web_commands` row; a wallet change is a ledger operation `web_command:<id>`. */
class WebCommandApplier(
    private val services: Services,
) {
    class Services(
        val dataSource: DataSource,
        val online: (Int) -> Player?,
        val pointTypes: () -> Set<Int>,
        val mottoMaxLength: () -> Int,
        val wordFilter: (String) -> String?,
        val badgeExists: (String) -> Boolean,
        val ranks: Ranks,
        val reloadSettings: () -> Unit,
        val reloadTvPlaylists: () -> Unit = {},
        val ledger: WalletLedger = WalletLedger.Standard,
        val invalidateBadgeCaches: () -> Unit,
        val rooms: () -> RoomPort,
        val accountLevelChanged: (Int) -> Unit = {},
        val kickBanned: (Int) -> Boolean = { false },
    )

    interface Ranks {
        fun exists(level: Int): Boolean

        fun stored(userId: Int): Int?

        fun holds(
            userId: Int,
            right: Privilege,
        ): Boolean

        @Throws(Exception::class)
        fun set(
            userId: Int,
            level: Int,
            actorId: Int?,
        )
    }

    private val wallet = WalletWebCommands(services)
    private val badges = BadgeWebCommands(services)
    private val profile = ProfileWebCommands(services)
    private val rooms = RoomWebCommands(services)
    private val privacy = PrivacyWebCommands(services)

    @Suppress("ReturnCount") // one early answer per refusal
    @Throws(SQLException::class)
    fun apply(command: WebCommand): WebCommandResult {
        if (command.userId <= 0) return WebCommandResult.failed("No user.")
        val type = command.type.orEmpty()
        if (type !in TYPES) return WebCommandResult.failed("Unknown type $type.")
        val payload =
            WebCommandPayload.parse(command.payload)
                ?: return WebCommandResult.failed("The payload is not a JSON object.")
        if (type != SET_RANK) senderRefusal(services.ranks, command, type)?.let { return WebCommandResult.failed(it) }
        return when (type) {
            SET_CURRENCY -> wallet.apply(command, payload, set = true)
            ADD_CURRENCY -> wallet.apply(command, payload, set = false)
            GIVE_BADGE -> badges.give(command, payload)
            TAKE_BADGE -> badges.take(command, payload)
            SET_RANK -> profile.setRank(command, payload)
            RELOAD_SETTINGS -> reloadSettings(command)
            RELOAD_TV_PLAYLISTS -> reloadTvPlaylists(command)
            ASK_ROOM_QUESTION -> rooms.askQuestion(payload)
            SET_PRIVACY -> privacy.set(command, payload)
            KICK_BANNED -> kickBanned(command)
            else -> profile.setMotto(command, payload)
        }
    }

    private fun reloadSettings(command: WebCommand): WebCommandResult {
        services.reloadSettings()
        log.info("settings read again for player {} (web command #{})", command.createdBy, command.id)
        return WebCommandResult.done("Settings reloaded.")
    }

    private fun reloadTvPlaylists(command: WebCommand): WebCommandResult {
        services.reloadTvPlaylists()
        log.info("TV playlists read again for player {} (web command #{})", command.createdBy, command.id)
        return WebCommandResult.done("TV playlists are being read again.")
    }

    private fun kickBanned(command: WebCommand): WebCommandResult =
        WebCommandResult.done(if (services.kickBanned(command.userId)) "Disconnected." else "Not online.")

    companion object {
        const val SET_CURRENCY = "setCurrency"
        const val ADD_CURRENCY = "addCurrency"
        const val GIVE_BADGE = "giveBadge"
        const val TAKE_BADGE = "takeBadge"
        const val SET_MOTTO = "setMotto"
        const val SET_RANK = "setRank"
        const val RELOAD_SETTINGS = "reloadSettings"
        const val RELOAD_TV_PLAYLISTS = "reloadTvPlaylists"
        const val ASK_ROOM_QUESTION = "askRoomQuestion"
        const val SET_PRIVACY = "setPrivacy"
        const val KICK_BANNED = "kickBanned"
        val TYPES: Set<String> =
            setOf(
                SET_CURRENCY,
                ADD_CURRENCY,
                GIVE_BADGE,
                TAKE_BADGE,
                SET_MOTTO,
                SET_RANK,
                RELOAD_SETTINGS,
                RELOAD_TV_PLAYLISTS,
                ASK_ROOM_QUESTION,
                SET_PRIVACY,
                KICK_BANNED,
            )

        const val VIA_HOUSEKEEPING = "housekeeping"
        private val log = LoggerFactory.getLogger(WebCommandApplier::class.java)

        internal val BADGE_CODE = Regex("[A-Za-z0-9_]{1,32}")
        internal const val GIVE_OFFLINE =
            "INSERT IGNORE INTO player_badges (player_id, slot, badge_code) SELECT id, 0, ? FROM players WHERE id = ?"
        internal const val TAKE_OFFLINE = "DELETE FROM player_badges WHERE player_id = ? AND badge_code = ?"
        private const val USER_EXISTS = "SELECT 1 FROM players WHERE id = ?"

        /** The staff right each staff kind needs of its sender. */
        private val SENDER_RIGHTS =
            mapOf(
                SET_CURRENCY to Privilege.HOUSEKEEPING_WALLET,
                ADD_CURRENCY to Privilege.HOUSEKEEPING_WALLET,
                GIVE_BADGE to Privilege.HOUSEKEEPING_BADGES,
                TAKE_BADGE to Privilege.HOUSEKEEPING_BADGES,
                SET_RANK to Privilege.HOUSEKEEPING_RANKS,
                RELOAD_SETTINGS to Privilege.HOUSEKEEPING_SETTINGS,
                RELOAD_TV_PLAYLISTS to Privilege.HOUSEKEEPING_YOUTUBE,
                ASK_ROOM_QUESTION to Privilege.MODERATION_ROOM_ALERT,
                KICK_BANNED to Privilege.MODERATION_PLAYER_BAN,
            )

        /** The kinds an account sends about itself, and nobody else about it. */
        private val OWN_KINDS = setOf(SET_MOTTO, SET_PRIVACY)

        /**
         * Why the sender may not send [type], or null. An own kind comes from the account itself: its row names the
         * account as sender, or nobody, as the site's own pages write it. Every other kind names a staff sender
         * who holds the support tool and the kind's right.
         */
        internal fun senderRefusal(
            ranks: Ranks,
            command: WebCommand,
            type: String,
        ): String? {
            val sender = command.createdBy
            if (type in OWN_KINDS) {
                return if (sender == null || sender == command.userId) null else "Only the account itself sends $type."
            }
            val right = SENDER_RIGHTS[type] ?: return "Nobody may send $type."
            if (sender == null) return "Only staff send $type."
            val allowed = ranks.holds(sender, Privilege.MODERATION_TOOL) && ranks.holds(sender, right)
            return if (allowed) null else "Player $sender may not send $type."
        }

        @Throws(SQLException::class)
        internal fun userExists(
            connection: Connection,
            userId: Int,
        ): Boolean = connection.first(USER_EXISTS, { int(userId) }) { true } == true
    }
}
