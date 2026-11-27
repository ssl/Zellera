package zel.room.unit.bot

import zel.game.bots.BotLimits
import zel.game.permissions.Privilege
import zel.game.players.Player
import zel.protocol.generated.BotErrorOut
import zel.protocol.generated.BotErrorOutCodec
import zel.protocol.generated.DanceOut
import zel.protocol.generated.DanceOutCodec
import zel.protocol.generated.PlayerNameChangedOut
import zel.protocol.generated.PlayerNameChangedOutCodec
import zel.protocol.generated.RoomUnitsOutCodec
import zel.room.RoomScope
import zel.room.chat.ChatText
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.UnitDirection
import zel.room.unit.walk.Walking

class BotSettings(
    private val bots: RoomBots,
    private val walking: Walking,
    private val filter: (Player, String) -> String = { _, text -> text },
    private val mottoLength: () -> Int = { DEFAULT_MOTTO_LENGTH },
) {
    fun apply(
        scope: RoomScope,
        player: Player,
        placed: RoomBot,
        skill: Int,
        data: String,
    ) {
        val bot = placed.bot
        if (!bot.ownerMayChange()) return
        when (skill) {
            LOOK -> copyLook(scope, player, placed)
            CHAT -> saveChat(player, placed, data)
            WALK -> bot.roams = !bot.roams
            DANCE -> nextDance(scope, placed)
            NAME -> rename(scope, player, placed, data)
            MOTTO -> setMotto(scope, player, placed, data)
            ROTATE -> rotate(scope, placed)
            else -> return
        }
        bots.changed(scope, placed)
    }

    private fun copyLook(
        scope: RoomScope,
        player: Player,
        placed: RoomBot,
    ) {
        placed.bot.figure = player.profile.figure
        placed.bot.gender = player.profile.gender
        resend(scope, placed)
    }

    /** The chat dialog sends its text, then the speak, gap and shuffle settings, each after a [SEPARATOR]. */
    private fun saveChat(
        player: Player,
        placed: RoomBot,
        data: String,
    ) {
        if (muted(player)) return
        val lineLength = minOf(ChatText.MAX_LENGTH, BotLimits.STORED_LINE_LENGTH)
        if (data.length > BotLimits.MAX_CHAT_LINES * (lineLength + 1) + SETTINGS_ROOM) return
        val parts = data.split(SEPARATOR)
        if (parts.size != CHAT_PARTS) return
        val lines =
            parts[TEXT]
                .lineSequence()
                .map { filter(player, it.replace(MARKUP, "")).trim().take(lineLength) }
                .filter { it.isNotEmpty() }
                .take(BotLimits.MAX_CHAT_LINES)
                .toList()
        val speech = placed.bot.speech
        speech.auto = parts[SPEAKS].toBoolean()
        speech.random = parts[SHUFFLED].toBoolean()
        speech.delaySeconds = parts[GAP].trim().toIntOrNull() ?: BotLimits.minChatDelay
        speech.replaceLines(lines)
        placed.chatIndex = 0
    }

    private fun nextDance(
        scope: RoomScope,
        placed: RoomBot,
    ) {
        placed.dance = (placed.dance + 1) % DANCES
        val dance = DanceOut(placed.unitIndex, placed.dance)
        scope.out.send(Lane.Now, Audience.Room, DanceOutCodec, dance)
    }

    private fun rename(
        scope: RoomScope,
        player: Player,
        placed: RoomBot,
        typed: String,
    ) {
        val name = typed.trim()
        val fits = name.isNotEmpty() && name.length <= BotLimits.maxNameLength && name.none { it in MARKUP_SIGNS }
        if (!fits || !name.equals(filter(player, name), ignoreCase = true)) {
            val error = BotErrorOut(NAME_REFUSED)
            scope.out.send(Lane.Now, Audience.Player(player.profile.id), BotErrorOutCodec, error)
            return
        }
        placed.bot.name = name
        val index = placed.unitIndex
        val changed = PlayerNameChangedOut(index, index, name)
        scope.out.send(Lane.Now, Audience.Room, PlayerNameChangedOutCodec, changed)
    }

    /** A bot's motto follows the players' motto rules: filtered, trimmed, refused past their limit. */
    private fun setMotto(
        scope: RoomScope,
        player: Player,
        placed: RoomBot,
        typed: String,
    ) {
        if (typed.length > MOTTO_SENT_AT_MOST) return
        val motto = filter(player, typed.replace(MARKUP, "")).trim()
        if (motto.length > minOf(mottoLength(), BotLimits.STORED_MOTTO_LENGTH)) return
        placed.bot.motto = motto
        resend(scope, placed)
    }

    private fun rotate(
        scope: RoomScope,
        placed: RoomBot,
    ) {
        val unit = scope.room.units[placed.unitIndex] ?: return
        unit.face((unit.bodyDirection + 2) % UnitDirection.COUNT)
        walking.sendNow(scope, listOf(unit))
    }

    private fun muted(player: Player): Boolean =
        !player.stats.standing.mayTalk() && !player.hasPermission(Privilege.CHAT_MUTE_IMMUNE)

    private fun resend(
        scope: RoomScope,
        placed: RoomBot,
    ) {
        val unit = scope.room.units[placed.unitIndex] ?: return
        scope.out.send(Lane.Now, Audience.Room, RoomUnitsOutCodec, BotEntries.users(listOf(placed to unit)))
    }

    private companion object {
        const val LOOK = 1
        const val CHAT = 2
        const val WALK = 3
        const val DANCE = 4
        const val NAME = 5
        const val MOTTO = 9
        const val ROTATE = 11
        const val DANCES = 5
        const val NAME_REFUSED = 4
        const val CHAT_PARTS = 4
        const val TEXT = 0
        const val SPEAKS = 1
        const val GAP = 2
        const val SHUFFLED = 3
        const val SETTINGS_ROOM = 64
        const val MOTTO_SENT_AT_MOST = 255
        const val DEFAULT_MOTTO_LENGTH = 38
        const val MARKUP_SIGNS = "<>"
        const val SEPARATOR = ";#;"
        val MARKUP = Regex("<[^>]*>")
    }
}
