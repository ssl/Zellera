package zel

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Lifecycle {
    private class Shutdown(
        val name: String,
        val run: () -> Unit,
    )

    private val shutdowns = mutableListOf<Shutdown>()
    private var closed = false

    fun <T> start(
        name: String,
        stop: (T) -> Unit = {},
        start: () -> T,
    ): T = start().also { built -> atStop(name) { stop(built) } }

    fun atStop(
        name: String,
        stop: () -> Unit,
    ) {
        synchronized(this) { shutdowns += Shutdown(name, stop) }
    }

    /** Latest first; a failing one logs and the rest still run. */
    fun stop() {
        val pending =
            synchronized(this) {
                if (closed) emptyList() else shutdowns.asReversed().toList().also { closed = true }
            }
        pending.forEach { shutdown ->
            runCatching(shutdown.run)
                .onSuccess { log.debug("{} stopped", shutdown.name) }
                .onFailure { log.error("{} did not stop cleanly", shutdown.name, it) }
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(Lifecycle::class.java)
    }
}
