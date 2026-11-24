package zel.protocol.incoming.guilds

import zel.game.guilds.Guild
import zel.game.guilds.GuildDirectory
import zel.game.guilds.MemberLevel
import zel.game.guilds.Membership
import zel.game.permissions.isAnyGroupOwner

internal class GuildRights(
    private val guild: Guild,
    private val caller: Caller,
    guilds: GuildDirectory,
) {
    val member: Membership? by lazy(LazyThreadSafetyMode.NONE) { guilds.members.find(guild, caller.player) }

    val isOwner: Boolean get() = guild.ownerId == caller.userId
    val isStaff: Boolean get() = caller.player.isAnyGroupOwner

    val mayManage: Boolean get() = isOwner || isStaff

    val mayModerate: Boolean get() = mayManage || member?.role?.isAdmin == true

    fun mayPreviewRemoval(targetId: Int): Boolean = targetId == caller.userId || mayModerate

    fun mayRemove(target: Membership): Boolean {
        val rank = member?.role
        if (rank == null && !isStaff) return false
        if (target.playerId == guild.ownerId) return false
        val actorOutranksAdmins = isOwner || rank == MemberLevel.FOUNDER || isStaff
        if (target.role.isAdmin && !actorOutranksAdmins) return false
        return target.playerId == caller.userId || actorOutranksAdmins || rank == MemberLevel.OFFICER
    }
}
