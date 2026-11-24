package zel.server

import zel.protocol.misc.LatencyPackets
import zel.protocol.typed.PacketPackage

fun gamePackages(wiring: ServerWiring): List<PacketPackage> =
    listOf(LatencyPackets()) +
        wiring.handshake.packages +
        wiring.misc.packages +
        wiring.guilds.packages +
        wiring.moderation.packages() +
        wiring.players.packages +
        listOf(
            wiring.bots,
            wiring.camera,
            wiring.crafting,
            wiring.discord,
            wiring.earnings,
            wiring.icons,
            wiring.landing.packets,
            wiring.featuredLooks,
            wiring.polls,
            wiring.safetyQuiz,
            wiring.selfDonation,
            wiring.treasureHunt,
        ) +
        wiring.snowball +
        wiring.navigator.packages +
        wiring.progression +
        wiring.social +
        wiring.catalog.packets() +
        wiring.roomEngine.packages
