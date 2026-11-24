package zel.protocol.incoming.guilds

import zel.game.guilds.Guild
import zel.game.guilds.GuildStanding
import zel.game.guilds.Membership
import zel.game.permissions.isAnyGroupOwner
import zel.game.players.Player
import zel.net.clients.ClientConnection
import zel.protocol.outgoing.guilds.GuildDetailsView

/** Staff see it as the owner does. */
internal class GuildPanel(
    private val services: GuildServices,
) {
    fun show(
        client: ClientConnection,
        guild: Guild,
        newWindow: Boolean,
        member: Membership?,
        viewer: Player,
    ) {
        val isOwner = guild.ownerId == viewer.profile.id || viewer.isAnyGroupOwner
        val isAdmin = isOwner || member?.role?.isAdmin == true
        val view =
            GuildDetailsView(
                status = member?.standing ?: GuildStanding.NONE,
                isFavourite = viewer.stats.favouriteGuild == guild.id,
                isOwner = isOwner,
                isAdmin = isAdmin,
                newWindow = newWindow,
                visibleRequests = if (isAdmin) guild.requestCount else 0,
            )
        services.messages.sendDetails(client, guild, view)
    }

    fun showToCaller(
        caller: Caller,
        guild: Guild,
    ) = show(caller.client, guild, false, services.guilds.members.find(guild, caller.player), caller.player)
}
