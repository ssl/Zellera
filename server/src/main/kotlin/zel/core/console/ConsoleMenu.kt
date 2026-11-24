package zel.core.console

import zel.core.ServerState
import zel.core.threading.GameThreads
import zel.game.Game
import zel.net.server.GamePort
import java.util.Locale
import kotlin.system.exitProcess

class ConsoleMenu(
    entries: List<ConsoleVerb>,
) {
    private val verbs: Map<String, ConsoleVerb> = entries.associateBy { it.verb.lowercase(Locale.ROOT) }

    val help: List<String> get() = verbs.values.map(ConsoleVerb::usage)

    fun entry(verb: String): ConsoleVerb? = verbs[verb.lowercase(Locale.ROOT)]

    fun dispatch(typed: String): Boolean {
        val parts = WORD.findAll(typed).map(MatchResult::value).toList()
        val entry = parts.firstOrNull()?.let(::entry) ?: return false
        entry.run(parts.subList(1, parts.size))
        return true
    }

    companion object {
        private val WORD = Regex("\\S+")

        fun standard(
            state: ServerState,
            version: String,
            hash: String,
            threading: () -> GameThreads?,
            gamePort: () -> GamePort?,
            game: () -> Game?,
        ): ConsoleMenu =
            ConsoleMenu(
                listOf(
                    StopCommand(threading, gamePort, game) { exitProcess(0) },
                    InfoCommand(state, version, hash) { game()?.players?.count ?: 0 },
                ),
            )
    }
}
