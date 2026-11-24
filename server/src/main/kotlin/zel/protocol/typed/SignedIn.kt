package zel.protocol.typed

import zel.game.players.Player
import zel.net.clients.ClientConnection

/** The player of a route that runs only signed in ([PacketRoute.requiresPlayer]). */
internal val ClientConnection.signedIn: Player
    get() = checkNotNull(player) { "route without a signed-in player" }

internal val PacketSession.player: Player
    get() = client.signedIn
