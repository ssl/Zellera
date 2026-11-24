package zel.protocol.typed

import zel.net.clients.ClientConnection

/**
 * A check of one allowed-value name: [now] answers from memory (null when it cannot tell), [later] reads the
 * database after the handler ran.
 */
class AllowedLookup(
    val now: (ClientConnection, Any) -> Boolean?,
    val later: ((Any) -> Boolean)? = null,
)
