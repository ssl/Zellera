package zel.game.webcommands

import org.slf4j.LoggerFactory
import java.sql.SQLException

/** Applies pending `web_commands` once a second, each at most once; rows left taken by an earlier run fail at start. */
class WebCommandProcessor(
    private val store: WebCommandStore,
    private val apply: (WebCommand) -> WebCommandResult?,
    private val clock: () -> Int,
    private val delay: Delay,
    private val ready: () -> Boolean,
) : Runnable {
    fun interface Delay {
        fun after(
            task: Runnable,
            delayMs: Long,
        )
    }

    @Volatile
    var isDisposed: Boolean = false
        private set

    // Only the single run loop touches it.
    private var swept = false

    fun start() {
        delay.after(this, INTERVAL_MS)
    }

    fun dispose() {
        isDisposed = true
    }

    @Suppress("TooGenericExceptionCaught") // a failed run must never end the loop
    override fun run() {
        if (isDisposed) return
        try {
            if (ready()) tick()
        } catch (failure: Throwable) {
            LOGGER.error("web_commands: run failed", failure)
        } finally {
            if (!isDisposed) delay.after(this, INTERVAL_MS)
        }
    }

    internal fun tick(): Int =
        try {
            sweepOnce()
            var finished = 0
            for (command in store.pending(BATCH)) {
                if (isDisposed) break
                if (processSafely(command)) finished++
            }
            finished
        } catch (failure: SQLException) {
            LOGGER.error("web_commands: the table could not be read", failure)
            0
        }

    private fun sweepOnce() {
        if (swept) return
        val interrupted = store.failInterrupted()
        swept = true
        if (interrupted > 0) LOGGER.warn("web_commands: {} interrupted command(s) marked failed", interrupted)
    }

    private fun processSafely(command: WebCommand): Boolean =
        try {
            process(command)
        } catch (failure: SQLException) {
            LOGGER.error("web_commands: #{} could not be taken", command.id, failure)
            false
        }

    @Suppress("TooGenericExceptionCaught") // any failure of one row becomes its failed result
    @Throws(SQLException::class)
    internal fun process(command: WebCommand): Boolean {
        if (!store.claim(command.id, clock())) return false
        val result =
            try {
                apply(command) ?: WebCommandResult.failed("Nothing was applied.")
            } catch (failure: Exception) {
                LOGGER.error(
                    "web_commands: #{} ({} for user {}) failed",
                    command.id,
                    command.type,
                    command.userId,
                    failure,
                )
                WebCommandResult.failed("The game could not apply it (${failure.javaClass.simpleName}).")
            }
        try {
            store.finish(command.id, result, clock())
        } catch (failure: SQLException) {
            // applied but not marked: the next start marks it failed (interrupted) instead of applying it again
            LOGGER.error("web_commands: #{} was {} but could not be marked", command.id, result.status(), failure)
        }
        return true
    }

    companion object {
        const val INTERVAL_MS = 1000L
        const val BATCH = 50
        private val LOGGER = LoggerFactory.getLogger(WebCommandProcessor::class.java)
    }
}
