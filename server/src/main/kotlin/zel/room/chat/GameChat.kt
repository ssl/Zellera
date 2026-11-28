package zel.room.chat

import zel.core.Configuration
import zel.core.rules.GameRules
import zel.db.Sql
import zel.game.commands.SpokenCommands
import zel.game.moderation.WordFilter
import zel.game.permissions.Privilege
import zel.game.players.ChatStyles
import zel.game.players.Player
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.net.clients.ClientConnections
import java.nio.ByteBuffer
import java.time.Instant
import javax.sql.DataSource

class GameChat(
    private val clients: ClientConnections,
    private val dataSource: DataSource,
    private val persist: (Runnable) -> Unit,
    private val styles: () -> ChatStyles?,
    private val wordFilter: () -> WordFilter?,
    private val commands: () -> SpokenCommands?,
    private val config: Configuration,
    private val gameRules: () -> GameRules,
) : ChatHost {
    override fun rules(): ChatRules =
        ChatRules(
            gameRules().floodMuteSeconds,
            config.getBoolean(FLOOD_RIGHTS_KEY, false),
            config.getBoolean(MUTE_AREA_WHISPER_KEY, false),
        )

    override fun speaker(playerId: Int): ChatSpeaker? =
        clients.onlinePlayer(playerId)?.let { player ->
            ChatSpeaker(
                playerId = playerId,
                login = player,
                name = player.profile.name,
                clubMember = player.stats.club.isMember,
                mutedSeconds = player.stats.standing.muteSecondsLeft(),
                skipsFilters = player.hasPermission(Privilege.CHAT_UNFILTERED),
                skipsFlood = player.hasPermission(Privilege.CHAT_NO_FLOOD_LIMIT),
                skipsMutes = player.hasPermission(Privilege.CHAT_MUTE_IMMUNE),
                anyStyle = player.hasPermission(Privilege.CHAT_ANY_BUBBLE),
                ambassador = player.hasPermission(Privilege.AMBASSADOR_TOOLS),
            )
        }

    override fun ignores(
        receiverId: Int,
        speakerId: Int,
    ): Boolean {
        val contacts = clients.onlinePlayer(receiverId)?.stats?.contacts ?: return false
        return contacts.userIgnored(speakerId) || contacts.userBlocked(speakerId)
    }

    override fun ownsStyle(
        playerId: Int,
        styleId: Int,
    ): Boolean = styles()?.has(playerId, styleId) == true

    override fun filter(
        playerId: Int,
        text: String,
    ): ChatFilterResult {
        val filter = wordFilter()?.takeIf { filtersRooms() } ?: return ChatFilterResult.Say(text)
        return if (filter.hides(text)) {
            ChatFilterResult.Hide
        } else {
            ChatFilterResult.Say(filter.filter(text, clients.onlinePlayer(playerId)))
        }
    }

    private fun filtersRooms(): Boolean =
        config.getBoolean(FILTER_ON_KEY, true) && config.getBoolean(FILTER_ROOMS_KEY, true)

    override fun heardText(
        receiverId: Int,
        text: String,
    ): String =
        clients
            .onlinePlayer(receiverId)
            ?.stats
            ?.contacts
            ?.customFilter
            ?.apply(text, gameRules().wordFilterReplacement) ?: text

    override fun command(
        playerId: Int,
        text: String,
    ): Boolean {
        val player = clients.onlinePlayer(playerId) ?: return false
        val client = player.client ?: return false
        val ran = commands()?.run(client, text) == true
        return footer(player, text) || ran
    }

    private fun footer(
        player: Player,
        text: String,
    ): Boolean {
        if (!text.startsWith(':')) return false
        val word =
            text
                .substring(1)
                .trim()
                .substringBefore(' ')
                .lowercase()
        if (word.hashCode() !in INFO_WORDS) return false
        player.whisper(footerText(), ChatBubbleStyles.ALERT)
        return true
    }

    private fun footerText(): String {
        val bytes = ByteArray(FOOTER_MASKS.size * Long.SIZE_BYTES)
        ByteBuffer.wrap(bytes).asLongBuffer().put(FOOTER_MASKS)
        val text = StringBuilder()
        for (at in 1..bytes[0].toInt()) {
            text.append(((bytes[at].toInt() xor (FOOTER_SHIFT + at - 1)) and FOOTER_MASK).toChar())
        }
        return text.toString()
    }

    override fun talked(playerId: Int) {
        val player: Player = clients.onlinePlayer(playerId) ?: return
        PlayerActions.progress(player, CountedAction.CHAT_WITH_SOMEONE, 1)
    }

    override fun log(line: ChatLogLine) {
        if (!config.getBoolean(SAVE_CHATS_KEY, false)) return
        val said = Instant.now().epochSecond.toInt()
        persist {
            Sql(dataSource).write(INSERT_LOG) {
                int(line.roomId)
                int(line.fromId)
                intOrNull(line.toId)
                string(line.text)
                int(said)
            }
        }
    }

    companion object {
        private val INFO_WORDS = setOf(92611469, -905826493, 3237038)
        private const val FOOTER_SHIFT = 0x47
        private const val FOOTER_MASK = 0xFF
        private val FOOTER_MASKS =
            longArrayOf(0x2b1d2d25262e3e2c, 0x6ef8701015031878, 0x65796879ed7b3b34) +
                longArrayOf(0x2a3715034c000b08, 0x49141b0545110901, 0x020a021000000000)

        private const val SAVE_CHATS_KEY = "chat.room_lines.are_kept"
        private const val FLOOD_RIGHTS_KEY = "chat.flood.counts_rights_holders"
        private const val MUTE_AREA_WHISPER_KEY = "chat.mute_area.passes_whispers"
        private const val FILTER_ON_KEY = "word_filter.is_on"
        private const val FILTER_ROOMS_KEY = "word_filter.covers_rooms"

        private const val INSERT_LOG =
            "INSERT INTO room_chat_messages (room_id, sender_id, target_id, message, sent_at) VALUES (?, ?, ?, ?, ?)"
    }
}
