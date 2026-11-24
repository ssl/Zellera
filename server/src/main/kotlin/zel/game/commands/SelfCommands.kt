package zel.game.commands

import zel.content.ClientStrings
import zel.core.Texts
import zel.game.effects.EffectRoles
import zel.game.players.PlayerSettings
import zel.room.chat.ChatBubbleStyles
import zel.room.port.RoomCommands

/** Commands that change only the speaker. */
class SelfCommands(
    private val texts: Texts,
    private val rooms: () -> RoomCommands,
    private val inRoom: (Int) -> Boolean,
) {
    fun all(): List<ChatCommand> =
        listOf(
            hearing(listOf("mutebots", "ignorebots", "mute_bots", "ignore_bots"), { it.mutedBots }) { settings, muted ->
                settings.mutedBots = muted
            },
            hearing(listOf("mutepets", "ignorepets", "mute_pets", "ignore_pets"), { it.mutedPets }) { settings, muted ->
                settings.mutedPets = muted
            },
            ChatCommand(listOf("moonwalk")) { said ->
                clubMove(said) { id ->
                    val moonwalk = EffectRoles.of(EffectRoles.MOONWALK)
                    rooms().giveEffect(id, if (rooms().effectOf(id) == moonwalk) 0 else moonwalk)
                }
            },
            ChatCommand(listOf("gangnam")) { said ->
                clubMove(
                    said,
                ) { id -> rooms().giveEffect(id, EffectRoles.of(EffectRoles.DANCE_FLOOR), DANCE_FLOOR_SECONDS) }
            },
            ChatCommand(listOf("mpgame")) { true },
        )

    /** The word the client profile gives the second club move, if it has one of its own. */
    fun clubMoveAlias(): String? =
        ClientStrings[ClientStrings.CLUB_DANCE_ALIAS]?.lowercase()?.takeIf { it.isNotEmpty() }

    /** The first of [words] names the answer texts. */
    private fun hearing(
        words: List<String>,
        muted: (PlayerSettings) -> Boolean,
        set: (PlayerSettings, Boolean) -> Unit,
    ) = ChatCommand(words) { said ->
        val settings = said.player.stats.settings
        val now = !muted(settings)
        set(settings, now)
        val answer = texts.text("command.${words.first()}." + if (now) "on" else "off")
        said.player.whisper(answer, ChatBubbleStyles.ALERT)
        true
    }

    /** A club move is said aloud by anyone not entitled. The first toggles; the second ends by itself. */
    private fun clubMove(
        said: Said,
        move: (Int) -> Unit,
    ): Boolean {
        val id = said.player.profile.id
        val entitled = said.player.stats.club.isMember && inRoom(id)
        if (entitled) move(id)
        return entitled
    }

    private companion object {
        const val DANCE_FLOOR_SECONDS = 30
    }
}
