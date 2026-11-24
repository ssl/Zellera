package zel.protocol.outgoing.guilds

import zel.game.guilds.GuildStanding

data class GuildDetailsView(
    val status: GuildStanding,
    val isFavourite: Boolean,
    val isOwner: Boolean,
    val isAdmin: Boolean,
    val newWindow: Boolean,
    val visibleRequests: Int,
)
