package zel.protocol.typed

import zel.net.clients.ClientConnection
import zel.util.monotonicMillis

/** A refused message does not restart the gap. */
class ClientCooldown(
    private val group: String,
    private val gapMillis: Long,
    private val clock: () -> Long = ::monotonicMillis,
) {
    fun acquire(client: ClientConnection): Boolean = client.allow(group, gapMillis, clock())
}
