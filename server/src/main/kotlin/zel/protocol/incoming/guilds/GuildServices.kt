package zel.protocol.incoming.guilds

import zel.core.Configuration
import zel.core.Texts
import zel.core.threading.GameThreads
import zel.game.guilds.BadgeImager
import zel.game.guilds.GuildDirectory
import zel.game.moderation.WordFilter
import zel.game.players.Players
import zel.protocol.outgoing.guilds.GuildMessages
import zel.protocol.outgoing.guilds.forums.ForumMessages
import zel.room.listing.RoomListings
import zel.room.port.RoomPort

class GuildServices(
    val guilds: GuildDirectory,
    val messages: GuildMessages,
    val forumMessages: ForumMessages,
    val players: Players,
    val rooms: RoomListings,
    val wordFilter: WordFilter,
    val config: Configuration,
    val texts: Texts,
    val threading: GameThreads,
    val badgeImager: () -> BadgeImager,
    val unixTime: () -> Int,
    val live: () -> RoomPort,
)
