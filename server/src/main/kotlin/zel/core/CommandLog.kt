package zel.core

import zel.db.writeEach
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.SQLException
import java.time.Instant
import javax.sql.DataSource

/**
 * The chat commands of ranks whose commands are logged. A run is noted in memory and [write] stores all that wait
 * in one batch, so a command never waits for the database. Runs a write could not store wait for the next one; with
 * [limit] waiting the oldest go, so a database that stays away costs log rows and never the server's memory.
 */
class CommandLog(
    private val limit: Int = DEFAULT_LIMIT,
    private val database: () -> DataSource?,
) {
    data class Run(
        val playerId: Int,
        val command: String,
        val line: String,
        val applied: Boolean,
        val at: Int = Instant.now().epochSecond.toInt(),
    )

    private val waiting = ArrayDeque<Run>()
    private var lost = 0

    fun note(run: Run) {
        synchronized(waiting) {
            waiting += run
            trim()
        }
    }

    /** How many runs were stored. */
    fun write(): Int {
        val target = database() ?: return 0
        val gone = synchronized(waiting) { lost.also { lost = 0 } }
        if (gone > 0) log.error("{} command runs were let go: more than {} waited to be written", gone, limit)
        val runs = synchronized(waiting) { waiting.toList().also { waiting.clear() } }
        if (runs.isEmpty()) return 0
        return try {
            target.connection.use { connection ->
                connection.writeEach(INSERT, runs) { run ->
                    int(run.at)
                    int(run.playerId)
                    string(run.command.take(COMMAND_CHARS))
                    string(run.line.take(LINE_CHARS))
                    boolean(run.applied)
                }
            }
            runs.size
        } catch (refusal: SQLException) {
            log.error("{} command runs not written; they wait for the next write", runs.size, refusal)
            synchronized(waiting) {
                waiting.addAll(0, runs)
                trim()
            }
            0
        }
    }

    private fun trim() {
        while (waiting.size > limit) {
            waiting.removeFirst()
            lost++
        }
    }

    companion object {
        const val LIMIT_KEY = "server.command_log.max_waiting"
        const val DEFAULT_LIMIT = 100_000
        const val INSERT =
            "INSERT INTO command_runs (ran_at, player_id, command, line, was_applied) VALUES (?, ?, ?, ?, ?)"
        private const val COMMAND_CHARS = 64
        private const val LINE_CHARS = 255
        private val log: Logger = LoggerFactory.getLogger(CommandLog::class.java)
    }
}
