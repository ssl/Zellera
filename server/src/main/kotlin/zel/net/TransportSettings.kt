package zel.net

import zel.core.Configuration
import io.netty.channel.WriteBufferWaterMark

/**
 * Unsent output per connection. Above [pauseBytes] a connection counts as behind and is watched until it is back
 * under [resumeBytes]; it closes when nothing left for [stalledSeconds] or more than [backlogBytes] wait.
 */
data class TransportSettings(
    val resumeBytes: Int = WriteBufferWaterMark.DEFAULT.low(),
    val pauseBytes: Int = WriteBufferWaterMark.DEFAULT.high(),
    val stalledSeconds: Int = DEFAULT_STALLED_SECONDS,
    val backlogBytes: Int = DEFAULT_BACKLOG_BYTES,
) {
    init {
        require(resumeBytes > 0) { "output resumes at a positive size, not $resumeBytes" }
        require(pauseBytes > resumeBytes) { "output pauses above the $resumeBytes it resumes at, not at $pauseBytes" }
        require(stalledSeconds > 0) { "a stall lasts a positive time, not $stalledSeconds" }
        require(backlogBytes >= pauseBytes) { "the backlog limit $backlogBytes lies under the pause size $pauseBytes" }
    }

    companion object {
        const val DEFAULT_STALLED_SECONDS = 90
        const val DEFAULT_BACKLOG_BYTES = 8 * 1024 * 1024
        private const val RESUME_KEY = "network.output.resume.bytes"
        private const val PAUSE_KEY = "network.output.pause.bytes"
        private const val STALLED_KEY = "network.output.stalled.seconds"
        private const val BACKLOG_KEY = "network.output.backlog.max.bytes"

        /** A value that is not positive takes its default; a size at or under the one before it becomes twice that. */
        fun read(settings: Configuration): TransportSettings {
            val defaults = TransportSettings()
            val resume = settings.getInt(RESUME_KEY).takeIf { it > 0 } ?: defaults.resumeBytes
            val pause = (settings.getInt(PAUSE_KEY).takeIf { it > 0 } ?: defaults.pauseBytes).takeIf { it > resume }
            val backlog = settings.getInt(BACKLOG_KEY).takeIf { it > 0 } ?: defaults.backlogBytes
            return TransportSettings(
                resume,
                pause ?: (resume * 2),
                settings.getInt(STALLED_KEY).takeIf { it > 0 } ?: defaults.stalledSeconds,
                backlog.coerceAtLeast(pause ?: (resume * 2)),
            )
        }
    }
}
