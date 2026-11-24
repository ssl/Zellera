package zel.core.console

import org.slf4j.LoggerFactory
import zel.core.ServerState
import zel.util.TimeStrings
import java.lang.management.ManagementFactory
import java.time.temporal.ChronoUnit

class InfoCommand(
    private val state: ServerState,
    private val version: String,
    private val build: String,
    private val playersOnline: () -> Int,
) : ConsoleVerb {
    override val verb = "info"
    override val usage = "info: version, uptime, players online and memory"

    override fun run(arguments: List<String>) = report().forEach { log.info(it) }

    fun report(): List<String> {
        val heap = ManagementFactory.getMemoryMXBean().heapMemoryUsage
        return listOf(
            if (build.isEmpty()) "version: $version" else "version: $version, build $build",
            "uptime: ${uptime(state.uptimeSeconds)}",
            "players online: ${playersOnline()}",
            "heap: ${heap.used.megabytes()} MB in use, ${heap.committed.megabytes()} MB held, " +
                "${heap.max.megabytes()} MB at most",
            "state: ${phase()}",
        )
    }

    private fun phase(): String =
        when {
            state.isShuttingDown -> "closing"
            state.isReady -> "serving"
            else -> "starting"
        }

    private fun Long.megabytes(): Long = this / BYTES_PER_MEGABYTE

    companion object {
        private val log = LoggerFactory.getLogger(InfoCommand::class.java)
        private const val BYTES_PER_MEGABYTE = 1024L * 1024

        /** Like `0h 1m 5s` or `1d 1h 0m 0s`. */
        fun uptime(seconds: Int): String = TimeStrings.short(seconds, ChronoUnit.SECONDS, shownFrom = ChronoUnit.HOURS)
    }
}
