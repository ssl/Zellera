package zel.protocol.incoming.users

import zel.protocol.typed.PacketPackage
import zel.server.GameServices

class PlayersModule(
    services: GameServices,
) {
    val packages: List<PacketPackage> =
        listOf(
            PlayerSettingsPackets(services, NewUserTour(services)),
            PlayerProfilePackets(services),
            NameChangePackets(services),
            InventoryPackets(services),
        )
}
