package zel.protocol.outgoing.moderation

import zel.util.GameZone
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** When a chat log line was said, in the game's zone: the time of day, with day and month before it unless today. */
object ChatLineTime {
    private val TODAY: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val EARLIER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")

    fun of(
        saidAt: Long,
        clock: Clock = GameZone.clock,
    ): String {
        val said = Instant.ofEpochSecond(saidAt).atZone(clock.zone)
        return (if (said.toLocalDate() == LocalDate.now(clock)) TODAY else EARLIER).format(said)
    }
}
