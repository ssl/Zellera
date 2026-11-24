package zel.protocol.incoming.guilds

import zel.protocol.generated.ExtendedProfileChangedOut
import zel.protocol.generated.ExtendedProfileChangedOutCodec
import zel.protocol.outgoing.users.UserProfile
import zel.protocol.typed.PacketIds

internal class GuildFavourites(
    services: GuildServices,
) {
    private val guilds = services.guilds
    private val messages = services.messages

    fun select(
        caller: Caller,
        guildId: Int,
    ) {
        val player = caller.player
        if (!PacketIds.isId(guildId) || guildId !in player.stats.guildIds) return
        val guild = guilds.byId(guildId)
        player.stats.favouriteGuild = guildId
        if (guild != null) {
            messages.broadcastBadge(player, guild)
            messages.broadcastFavourite(player, guild)
        }
        caller.client.send(UserProfile(player, player))
    }

    fun clear(
        caller: Caller,
        guildId: Int,
    ) {
        val player = caller.player
        if (!PacketIds.isId(guildId) || guildId !in player.stats.guildIds) return
        val guild = guilds.byId(guildId)
        player.stats.favouriteGuild = 0
        if (guild != null) messages.broadcastFavourite(player, null)
        caller.client.send(ExtendedProfileChangedOutCodec, ExtendedProfileChangedOut(caller.userId))
    }
}
