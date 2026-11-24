package zel.game

import zel.net.clients.ClientConnection
import zel.net.clients.ClientConnections
import zel.protocol.generated.InfoGameClosedOut
import zel.protocol.generated.InfoGameClosedOutCodec
import zel.protocol.generated.InfoGameClosingOut
import zel.protocol.generated.InfoGameClosingOutCodec
import zel.util.GameZone
import java.time.Duration
import java.time.LocalTime

class OpeningHoursWatch(
    private val hours: OpeningHours,
    private val maintenance: Maintenance,
    private val clients: () -> ClientConnections?,
    private val clock: () -> LocalTime = { LocalTime.now(GameZone.clock) },
) {
    private var wasOpen: Boolean? = null

    fun look() = tick(clock())

    internal fun tick(now: LocalTime) {
        val open = hours.isOpenAt(now)
        val before = wasOpen
        wasOpen = open
        if (!hours.enabled) return
        val all = clients() ?: return
        when {
            before == true && !open -> {
                closeTheDoors(all)
            }

            open -> {
                val left = hours.minutesUntilClose(now)
                if (left != null &&
                    left in WARNING_MINUTES
                ) {
                    all.broadcast(InfoGameClosingOutCodec, InfoGameClosingOut(left))
                }
            }
        }
    }

    private fun closeTheDoors(all: ClientConnections) {
        val (staff, visitors) = all.connections.partition(::isStaff)
        staff.forEach {
            it.send(
                InfoGameClosedOutCodec,
                InfoGameClosedOut(hours.openHour, hours.openMinute, false),
            )
        }
        visitors.forEach {
            it.send(InfoGameClosedOutCodec, InfoGameClosedOut(hours.openHour, hours.openMinute, true))
            all.close(it)
        }
    }

    private fun isStaff(connection: ClientConnection): Boolean {
        val rank = connection.player?.profile?.rank ?: return false
        return maintenance.letsThrough(rank.id)
    }

    companion object {
        /** The warnings name whole minutes, so each minute is looked at once. */
        val EVERY: Duration = Duration.ofMinutes(1)
        private val WARNING_MINUTES = setOf(5, 1)
    }
}
