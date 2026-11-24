package zel.protocol.misc

import zel.game.achievements.Achievements
import zel.game.achievements.resolution.Resolutions
import zel.protocol.resolution.ResolutionPackets
import zel.protocol.typed.PacketPackage

class MiscModule(
    resolutions: Resolutions,
    now: () -> Int,
    achievements: () -> Achievements?,
) {
    val packages: List<PacketPackage> =
        listOf(MiscPackets(achievements), ResolutionPackets(resolutions, now))
}
