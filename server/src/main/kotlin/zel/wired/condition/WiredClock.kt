package zel.wired.condition

import zel.room.Room
import zel.util.GameZone
import java.time.Clock
import java.time.DateTimeException
import java.time.ZoneId
import java.time.ZonedDateTime

class WiredClock(
    private val zoneName: (Room) -> String,
    private val fixedZone: ZoneId? = null,
    private val clock: Clock = Clock.systemUTC(),
) {
    /** A room without a zone of its own keeps the game's local time. */
    private val gameZone: ZoneId get() = fixedZone ?: GameZone.current

    fun now(room: Room): ZonedDateTime = ZonedDateTime.now(clock.withZone(zone(room)))

    private fun zone(room: Room): ZoneId =
        try {
            zoneName(room).takeIf { it.isNotBlank() }?.let(ZoneId::of) ?: gameZone
        } catch (_: DateTimeException) {
            gameZone
        }
}
