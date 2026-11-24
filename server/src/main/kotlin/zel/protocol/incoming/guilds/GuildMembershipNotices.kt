package zel.protocol.incoming.guilds

import zel.game.guilds.Guild
import zel.game.players.Player
import zel.game.players.connection
import zel.net.clients.ClientConnection
import zel.protocol.generated.ExtendedProfileChangedOut
import zel.protocol.generated.ExtendedProfileChangedOutCodec

class GuildMembershipNotices(
    private val services: GuildServices,
) {
    private val guilds = services.guilds
    private val messages = services.messages
    private val panel = GuildPanel(services)

    fun approved(
        guild: Guild,
        userId: Int,
        member: Player?,
        approver: ClientConnection?,
    ) {
        val row = guilds.members.find(guild.id, userId)
        if (row != null && approver != null) messages.sendMemberUpdate(approver, guild, row)
        if (member != null) refreshInGroupRoom(member, guild)
        if (member != null && member.stats.favouriteGuild == 0) {
            member.stats.favouriteGuild = guild.id
            announceFavourite(member, guild)
        }
        approver?.send(ExtendedProfileChangedOutCodec, ExtendedProfileChangedOut(userId))
        member?.client?.let { messages.sendMemberships(it, guilds.parts, groupsOf(member, guilds), member) }
    }

    fun joined(
        guild: Guild,
        player: Player,
    ) {
        val client = player.connection
        refreshInGroupRoom(player, guild)
        val favourite = player.stats.favouriteGuild == 0
        if (favourite) player.stats.favouriteGuild = guild.id
        messages.sendMemberships(client, guilds.parts, groupsOf(player, guilds), player)
        panel.show(client, guild, false, guilds.members.find(guild, player), player)
        if (favourite) announceFavourite(player, guild)
        client.send(ExtendedProfileChangedOutCodec, ExtendedProfileChangedOut(player.profile.id))
    }

    private fun refreshInGroupRoom(
        player: Player,
        guild: Guild,
    ) {
        val live = services.live()
        val playerId = player.profile.id
        val roomId = live.roomOf(playerId) ?: return
        if (live.groupOf(roomId) != guild.id) return
        live.groupRightsChanged(playerId)
    }

    private fun announceFavourite(
        player: Player,
        guild: Guild,
    ) {
        messages.broadcastBadge(player, guild)
        messages.broadcastFavourite(player, guild)
    }
}
