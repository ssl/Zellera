package zel.game.moderation

import zel.core.Configuration
import zel.core.Texts
import zel.core.threading.GameThreads
import zel.game.items.OwnedFurni
import zel.game.permissions.Permissions
import zel.game.players.Players
import zel.net.clients.ClientConnections
import zel.protocol.outgoing.moderation.ModerationOutbox
import zel.room.port.RoomPort
import javax.sql.DataSource

@Suppress("LongParameterList") // the server services the three are built from
class ModerationModule(
    dataSource: DataSource,
    config: Configuration,
    texts: Texts,
    players: Players,
    permissions: Permissions,
    rooms: () -> RoomPort,
    threading: GameThreads,
    outbox: ModerationOutbox,
    clients: () -> ClientConnections,
    loadPhoto: (Int) -> OwnedFurni?,
    clock: () -> Int,
) {
    val service =
        ModerationService(
            dataSource,
            config,
            texts,
            players,
            permissions,
            rooms,
            clients,
            threading,
            outbox,
            loadPhoto,
            clock,
        )
    val sanctions = Sanctions(dataSource, players, service, config, clock)
    val wordFilter = WordFilter(config, dataSource)
}
