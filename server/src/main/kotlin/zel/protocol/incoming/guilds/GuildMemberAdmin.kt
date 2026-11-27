package zel.protocol.incoming.guilds

import zel.game.guilds.Guild
import zel.game.guilds.GuildJoinFailure
import zel.game.guilds.GuildMemberFailure
import zel.game.guilds.GuildMemberships
import zel.game.guilds.MemberLevel
import zel.game.players.Player
import zel.net.clients.ClientConnection
import zel.protocol.generated.ExtendedProfileChangedOut
import zel.protocol.generated.ExtendedProfileChangedOutCodec
import zel.protocol.typed.PacketIds

internal class GuildMemberAdmin(
    private val services: GuildServices,
    private val notices: GuildMembershipNotices,
    private val panel: GuildPanel,
) {
    private val guilds = services.guilds
    private val messages = services.messages

    private fun Caller.rights(guild: Guild) = GuildRights(guild, this, guilds)

    fun join(
        caller: Caller,
        guildId: Int,
    ) {
        if (!PacketIds.isId(guildId) || guildId in caller.player.stats.guildIds) return
        val guild = guilds.byId(guildId) ?: return
        if (guild.state.isInviteOnly) {
            messages.sendJoinFailed(caller.client, GuildJoinFailure.CLOSED)
            return
        }
        guilds.members.request(guild, caller.player)?.let { messages.sendJoinFailed(caller.client, it) }
        val member = guilds.members.find(guild, caller.player)
        if (member != null && member.role != MemberLevel.APPLICANT) {
            notices.joined(guild, caller.player)
            return
        }
        panel.show(caller.client, guild, false, member, caller.player)
        if (member != null) {
            services.players.find(guild.ownerId)?.client?.let {
                messages.sendMembershipRequested(it, guild, caller.player)
            }
        }
    }

    fun approve(
        caller: Caller,
        guildId: Int,
        targetId: Int,
    ) {
        if (!GuildInput.areIds(guildId, targetId)) return
        val guild = guilds.byId(guildId) ?: return
        if (!caller.rights(guild).mayModerate) return
        val target = guilds.members.find(guildId, targetId)
        when {
            target == null -> {
                messages.sendMemberError(caller.client, guild.id, GuildMemberFailure.NO_LONGER_MEMBER)
            }

            target.role != MemberLevel.APPLICANT -> {
                messages.sendMemberError(
                    caller.client,
                    guild.id,
                    GuildMemberFailure.ALREADY_ACCEPTED,
                )
            }

            else -> {
                accept(caller, guild, targetId)
            }
        }
    }

    fun approveAll(
        caller: Caller,
        guildId: Int,
    ) {
        val guild = guilds.byValidId(guildId) ?: return
        if (!caller.rights(guild).mayModerate) return
        val applicants =
            guilds
                .allMembers(guildId)
                .filter { it.role == MemberLevel.APPLICANT }
                .take(MAX_ACCEPTED_PER_REQUEST)
                .map { it.playerId }
        if (applicants.isEmpty()) return
        guilds.members.acceptAll(guild, applicants).zip(applicants).forEach { (result, targetId) ->
            settle(caller, guild, targetId, result)
        }
    }

    private fun accept(
        caller: Caller,
        guild: Guild,
        targetId: Int,
    ) = settle(caller, guild, targetId, guilds.members.accept(guild, targetId))

    private fun settle(
        caller: Caller,
        guild: Guild,
        targetId: Int,
        result: GuildMemberships.Acceptance,
    ) {
        when (result) {
            GuildMemberships.Acceptance.Accepted -> {
                val target = services.players.find(targetId)
                target?.stats?.addGuild(guild.id)
                guild.requestSettled()
                guild.memberJoined()
                notices.approved(guild, targetId, target, caller.client)
            }

            is GuildMemberships.Acceptance.Refused -> {
                messages.sendJoinFailed(caller.client, result.reason)
            }

            GuildMemberships.Acceptance.Failed -> {}
        }
    }

    fun decline(
        caller: Caller,
        guildId: Int,
        targetId: Int,
    ) {
        if (!GuildInput.areIds(guildId, targetId)) return
        val guild = guilds.byId(guildId) ?: return
        if (targetId != caller.userId && !caller.rights(guild).mayModerate) return
        if (guilds.members.find(guildId, targetId)?.role != MemberLevel.APPLICANT) return
        guild.requestSettled()
        guilds.members.remove(guild, targetId)
        messages.sendMembershipRejected(caller.client, guild, targetId)
    }

    fun setAdmin(
        caller: Caller,
        guildId: Int,
        targetId: Int,
    ) {
        if (!GuildInput.areIds(guildId, targetId)) return
        val guild = guilds.byId(guildId) ?: return
        if (!caller.rights(guild).mayManage) return
        guilds.members.makeAdmin(guild, targetId)
        if (services.players.find(targetId) != null) refreshIfInGroupRoom(targetId, guildId)
        sendRow(caller, guild, targetId)
    }

    fun demoteOfficer(
        caller: Caller,
        guildId: Int,
        targetId: Int,
    ) {
        if (!GuildInput.areIds(guildId, targetId)) return
        val guild = guilds.byId(guildId) ?: return
        if (!caller.rights(guild).mayManage) return
        val target = services.players.find(targetId)
        guilds.members.dropAdmin(guild, targetId)
        if (target != null) {
            target.client?.let { panel.show(it, guild, false, guilds.members.find(guild.id, targetId), target) }
            refreshIfInGroupRoom(targetId, guildId)
        }
        sendRow(caller, guild, targetId)
    }

    private fun sendRow(
        caller: Caller,
        guild: Guild,
        targetId: Int,
    ) {
        guilds.members.find(guild.id, targetId)?.let { messages.sendMemberUpdate(caller.client, guild, it) }
    }

    fun removeMember(
        caller: Caller,
        guildId: Int,
        targetId: Int,
    ) {
        if (!GuildInput.areIds(guildId, targetId)) return
        val guild = guilds.byId(guildId) ?: return
        val row = guilds.members.find(guildId, targetId) ?: return
        if (!caller.rights(guild).mayRemove(row)) return
        val target = services.players.find(targetId)
        val wasFavourite = target?.stats?.favouriteGuild == guildId
        guilds.members.remove(guild, targetId)
        if (row.role == MemberLevel.APPLICANT) guild.requestSettled() else guild.memberLeft()
        val live = services.live()
        val stillTheGroupRoom = live.summary(guild.roomId)?.groupId == guildId
        if (stillTheGroupRoom) live.ejectFurniOf(guild.roomId, targetId)
        if (target != null) release(target, guild, wasFavourite) else guilds.members.clearFavourite(targetId, guildId)
        for (client in removalNoticeClients(caller.client, target?.client)) {
            messages.sendMembershipRejected(client, guild, targetId)
            client.send(ExtendedProfileChangedOutCodec, ExtendedProfileChangedOut(targetId))
            messages.sendDetailsChanged(client, guild.id)
        }
        if (target != null && stillTheGroupRoom) refreshIfInGroupRoom(target.profile.id, guild.id)
    }

    private fun release(
        target: Player,
        guild: Guild,
        wasFavourite: Boolean,
    ) {
        target.stats.removeGuild(guild.id)
        if (target.stats.favouriteGuild == guild.id) target.stats.favouriteGuild = 0
        if (wasFavourite) messages.broadcastFavourite(target, null)
    }

    private fun refreshIfInGroupRoom(
        playerId: Int,
        guildId: Int,
    ) {
        val live = services.live()
        val roomId = live.roomOf(playerId) ?: return
        if (live.groupOf(roomId) == guildId) live.groupRightsChanged(playerId)
    }

    fun unblock(
        caller: Caller,
        guildId: Int,
        targetId: Int,
    ) {
        if (!GuildInput.areIds(guildId, targetId)) return
        val guild = guilds.byId(guildId) ?: return
        if (caller.rights(guild).mayModerate) guilds.members.unblock(guild, targetId)
    }

    fun furniOfMember(
        caller: Caller,
        guildId: Int,
        targetId: Int,
    ) {
        if (!GuildInput.areIds(guildId, targetId)) return
        val guild = guilds.byId(guildId) ?: return
        if (caller.rights(guild).mayPreviewRemoval(targetId)) {
            messages.sendConfirmRemoveMember(caller.client, targetId, services.rooms.countFurni(guild.roomId, targetId))
        }
    }

    private companion object {
        const val MAX_ACCEPTED_PER_REQUEST = 250
    }
}

internal fun removalNoticeClients(
    remover: ClientConnection,
    removed: ClientConnection?,
): List<ClientConnection> = listOfNotNull(remover, removed?.takeIf { it !== remover })
