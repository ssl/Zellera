package zel.game.commands

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.core.CommandLog
import zel.game.players.Player
import zel.net.clients.ClientConnection

class SpokenCommands(
    roster: List<ChatCommand>,
    private val runLog: () -> CommandLog,
    private val clubMoveAlias: () -> String? = { null },
) {
    private val byWord: Map<String, ChatCommand> =
        roster.flatMap { command -> command.words.map { it to command } }.toMap()

    /**
     * True when [line] ran as a command, so it is not said. A line that names no command, or one the player may not
     * use, is said like any line: nobody learns from the answer which staff commands exist.
     */
    fun run(
        client: ClientConnection,
        line: String,
    ): Boolean {
        val player = client.player ?: return false
        if (!line.startsWith(PREFIX)) return false
        val words = line.substring(PREFIX.length).split(' ', '\t').filter(String::isNotEmpty)
        val command = words.firstOrNull()?.lowercase()?.let(::commandFor) ?: return false
        if (!player.mayUse(command)) return false
        val applied = attempt(command, Said(player, words.drop(1)))
        if (player.profile.rank.logsCommands) {
            runLog().note(CommandLog.Run(player.profile.id, command.name, line, applied))
        }
        return true
    }

    private fun commandFor(word: String): ChatCommand? =
        byWord[word] ?: byWord[CLUB_MOVE_WORD]?.takeIf { word == clubMoveAlias() }

    private fun Player.mayUse(command: ChatCommand): Boolean = command.needs?.let(::hasPermission) ?: true

    @Suppress("TooGenericExceptionCaught") // a command may fail in any way; the chat line still counts as handled
    private fun attempt(
        command: ChatCommand,
        said: Said,
    ): Boolean =
        try {
            command.run(said)
        } catch (failure: RuntimeException) {
            log.error("command {} failed", command.name, failure)
            false
        }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(SpokenCommands::class.java)
        const val PREFIX = ":"
        const val CLUB_MOVE_WORD = "gangnam"
    }
}
