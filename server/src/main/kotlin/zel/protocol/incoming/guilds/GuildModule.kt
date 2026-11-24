package zel.protocol.incoming.guilds

import zel.core.Configuration
import zel.core.Texts
import zel.core.threading.GameThreads
import zel.game.guilds.BadgeImager
import zel.game.guilds.GuildDirectory
import zel.game.moderation.WordFilter
import zel.game.players.Players
import zel.protocol.outgoing.guilds.forums.ForumMessages
import zel.protocol.typed.PacketPackage
import zel.room.listing.RoomListings
import zel.room.port.RoomPort

class GuildModule(
    guilds: GuildDirectory,
    players: Players,
    rooms: RoomListings,
    wordFilter: WordFilter,
    config: Configuration,
    texts: Texts,
    threading: GameThreads,
    badgeImager: () -> BadgeImager,
    unixTime: () -> Int,
    live: () -> RoomPort,
) {
    private val services =
        GuildServices(
            guilds,
            guilds.messages,
            ForumMessages(),
            players,
            rooms,
            wordFilter,
            config,
            texts,
            threading,
            badgeImager,
            unixTime,
            live,
        )

    val packages: List<PacketPackage> =
        listOf(
            GuildPackets(services, GuildMembershipNotices(services)),
            ForumPackets(services),
        )
}
