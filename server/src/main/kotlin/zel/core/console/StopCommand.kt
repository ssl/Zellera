package zel.core.console

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.core.threading.GameThreads
import zel.game.Game
import zel.net.server.GamePort
import zel.protocol.generated.InfoGameClosingOut
import zel.protocol.generated.InfoGameClosingOutCodec
import java.time.Duration
import java.time.Instant

class StopCommand(
    private val threading: () -> GameThreads?,
    private val gamePort: () -> GamePort?,
    private val game: () -> Game?,
    private val endProcess: () -> Unit,
) : ConsoleVerb {
    override val verb = "stop"
    override val usage = "stop [minutes]: stop the server, after a warning that long ahead"

    override fun run(arguments: List<String>) {
        val typed = arguments.firstOrNull()
        val minutes = typed?.toIntOrNull()
        if (typed != null && (minutes == null || minutes <= 0)) {
            log.warn("usage: {}", usage)
            return
        }
        val pool = threading()
        if (minutes == null || pool == null) {
            endProcess()
            return
        }
        val notice = Duration.ofMinutes(minutes.toLong())
        game()?.shutdownSchedule?.closesAt =
            Instant
                .now()
                .plus(notice)
                .epochSecond
                .toInt()
        gamePort()?.connections?.broadcast(InfoGameClosingOutCodec, InfoGameClosingOut(minutes))
        log.info("stopping in {} minute(s)", minutes)
        // The end runs on a thread of its own: the shutdown waits for the game pool to drain, this task included.
        pool.run(Runnable { Thread(endProcess, "stop").start() }, notice.toMillis())
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(StopCommand::class.java)
    }
}
