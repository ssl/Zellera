package zel.game.messenger

import zel.game.players.Player
import zel.net.clients.ClientConnections
import java.util.function.Supplier

open class ConnectedPlayers(
    private val clients: Supplier<ClientConnections>,
) {
    open fun byId(id: Int): Player? = clients.get().onlinePlayer(id)

    open fun byName(name: String): Player? = clients.get().onlinePlayer(name)
}
