package zel.game.navigation

import zel.core.Configuration
import zel.db.Database
import zel.game.Game
import zel.protocol.navigator.NavigatorPackets
import zel.protocol.navigator.NavigatorSearch
import zel.protocol.navigator.RoomCreation
import zel.protocol.navigator.RoomDeletion
import zel.protocol.navigator.RoomForwardPackets
import zel.protocol.navigator.RoomLifecyclePackets
import zel.protocol.navigator.RoomSearchPackets
import zel.protocol.navigator.RoomSearches
import zel.protocol.typed.PacketPackage

class NavigatorModule(
    game: Game,
    config: Configuration,
    database: Database,
) {
    val packages: List<PacketPackage>

    init {
        val rooms = game.roomListings
        val navigator = requireNotNull(game.navigator)
        packages =
            listOf(
                NavigatorPackets(rooms, config) { game.rooms },
                RoomSearchPackets(
                    rooms,
                    RoomSearches(rooms, navigator),
                    NavigatorSearch(rooms, navigator),
                    navigator,
                ),
                RoomForwardPackets(rooms, navigator, config) { game.rooms },
                RoomLifecyclePackets(
                    rooms,
                    RoomCreation(rooms, requireNotNull(game.wordFilter)),
                    RoomDeletion(
                        requireNotNull(game.guilds),
                        database,
                    ) { game.rooms },
                ),
            )
    }
}
