package zel.game.landing

import zel.core.Configuration
import zel.protocol.landing.LandingPackets
import zel.protocol.typed.PacketPackage

class LandingModule(
    manager: Landing,
    config: Configuration,
    now: () -> Int,
) {
    val packets: PacketPackage = LandingPackets(manager, config, now)
}
