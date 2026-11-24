package zel.game.commands

import zel.game.permissions.Privilege
import zel.game.players.Player

/** What a player said after the colon: [args] are the words after the command word. */
class Said(
    val player: Player,
    val args: List<String>,
)

/** [words] are lower case, the first is the name the run log keeps; [act] answers whether the command applied. */
class ChatCommand(
    val words: List<String>,
    val needs: Privilege? = null,
    private val act: (Said) -> Boolean,
) {
    val name: String get() = words.first()

    fun run(said: Said): Boolean = act(said)
}
