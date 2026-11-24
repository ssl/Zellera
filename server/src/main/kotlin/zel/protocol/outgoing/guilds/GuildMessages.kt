package zel.protocol.outgoing.guilds

import zel.game.guilds.Guild
import zel.game.guilds.GuildBadgeCode
import zel.game.guilds.GuildEditFailure
import zel.game.guilds.GuildJoinFailure
import zel.game.guilds.GuildMemberFailure
import zel.game.guilds.GuildPartCatalog
import zel.game.guilds.MemberLevel
import zel.game.guilds.Membership
import zel.game.players.Player
import zel.net.clients.ClientConnection
import zel.protocol.generated.FavoriteMembershipUpdateOut
import zel.protocol.generated.FavoriteMembershipUpdateOutCodec
import zel.protocol.generated.GroupBadgesOut
import zel.protocol.generated.GroupBadgesOutCodec
import zel.protocol.generated.GroupDeactivatedOut
import zel.protocol.generated.GroupDeactivatedOutCodec
import zel.protocol.generated.GroupDetailsChangedOut
import zel.protocol.generated.GroupDetailsChangedOutCodec
import zel.protocol.generated.GroupDetailsOut
import zel.protocol.generated.GroupDetailsOutCodec
import zel.protocol.generated.GroupJoinFailedOut
import zel.protocol.generated.GroupJoinFailedOutCodec
import zel.protocol.generated.GroupMembershipRequestedOut
import zel.protocol.generated.GroupMembershipRequestedOutCodec
import zel.protocol.generated.GuildCreatedOut
import zel.protocol.generated.GuildCreatedOutCodec
import zel.protocol.generated.GuildCreationInfoOut
import zel.protocol.generated.GuildCreationInfoOutCodec
import zel.protocol.generated.GuildEditFailedOut
import zel.protocol.generated.GuildEditFailedOutCodec
import zel.protocol.generated.GuildEditInfoOut
import zel.protocol.generated.GuildEditInfoOutCodec
import zel.protocol.generated.GuildEditorDataOut
import zel.protocol.generated.GuildEditorDataOutCodec
import zel.protocol.generated.GuildFurniContextMenuInfoOut
import zel.protocol.generated.GuildFurniContextMenuInfoOutCodec
import zel.protocol.generated.GuildMemberFurniCountInHQOut
import zel.protocol.generated.GuildMemberFurniCountInHQOutCodec
import zel.protocol.generated.GuildMemberMgmtFailedOut
import zel.protocol.generated.GuildMemberMgmtFailedOutCodec
import zel.protocol.generated.GuildMembersOut
import zel.protocol.generated.GuildMembersOutCodec
import zel.protocol.generated.GuildMembershipRejectedOut
import zel.protocol.generated.GuildMembershipRejectedOutCodec
import zel.protocol.generated.GuildMembershipUpdatedOut
import zel.protocol.generated.GuildMembershipUpdatedOutCodec
import zel.protocol.generated.GuildMembershipsOut
import zel.protocol.generated.GuildMembershipsOutCodec
import zel.room.listing.ListedRoom
import zel.util.GameZone
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

@Suppress("TooManyFunctions") // one sender per message
class GuildMessages {
    fun sendCreationInfo(
        client: ClientConnection,
        price: Int,
        rooms: List<ListedRoom>,
    ) = client.send(
        GuildCreationInfoOutCodec,
        GuildCreationInfoOut(
            price,
            rooms.map { GuildCreationInfoOut.List1Item(it.id, it.name, false) },
            DEFAULT_BADGE.map { (part, colour, position) -> GuildCreationInfoOut.List2Item(part, colour, position) },
        ),
    )

    fun sendEditorData(
        client: ClientConnection,
        parts: GuildPartCatalog,
    ) = client.send(
        GuildEditorDataOutCodec,
        GuildEditorDataOut(
            parts.bases.map { GuildEditorDataOut.List1Item(it.id, it.asset, it.assetMask) },
            parts.symbols.map { GuildEditorDataOut.List2Item(it.id, it.asset, it.assetMask) },
            parts.baseColors.map { GuildEditorDataOut.List3Item(it.id, it.asset) },
            parts.symbolColors.map { GuildEditorDataOut.List4Item(it.id, it.asset) },
            parts.backgroundColors.map { GuildEditorDataOut.List5Item(it.id, it.asset) },
        ),
    )

    fun sendCreated(
        client: ClientConnection,
        guild: Guild,
    ) = client.send(GuildCreatedOutCodec, GuildCreatedOut(guild.roomId, guild.id))

    fun sendEditFailed(
        client: ClientConnection,
        reason: GuildEditFailure,
    ) = client.send(GuildEditFailedOutCodec, GuildEditFailedOut(reason.code))

    /** Always says owner, since only managers open it; [rooms] never hold the group's own room. */
    fun sendEditInfo(
        client: ClientConnection,
        guild: Guild,
        rooms: List<ListedRoom>,
    ) = client.send(
        GuildEditInfoOutCodec,
        GuildEditInfoOut(
            rooms.filter { it.id != guild.roomId }.map { GuildEditInfoOut.List1Item(it.id, it.name, false) },
            true,
            guild.id,
            guild.name,
            guild.description,
            guild.roomId,
            guild.colorOne,
            guild.colorTwo,
            guild.state.code,
            if (guild.rights) RIGHTS_MEMBERS else RIGHTS_ADMINS,
            false,
            "",
            badgeParts(guild.badge).map { (part, colour, position) ->
                GuildEditInfoOut.List2Item(part, colour, position)
            },
            guild.badge,
            guild.memberCount,
        ),
    )

    fun sendDetails(
        client: ClientConnection,
        guild: Guild,
        view: GuildDetailsView,
    ) = client.send(
        GroupDetailsOutCodec,
        GroupDetailsOut(
            guild.id,
            true,
            guild.state.code,
            guild.name,
            guild.description,
            guild.badge,
            guild.roomId,
            guild.roomName,
            view.status.code,
            guild.memberCount,
            view.isFavourite,
            creationDate(guild.createdAt),
            view.isOwner,
            view.isAdmin,
            guild.ownerName,
            view.newWindow,
            guild.rights,
            view.visibleRequests,
            guild.hasForum,
        ),
    )

    fun sendDetailsChanged(
        client: ClientConnection,
        guildId: Int,
    ) = client.send(GroupDetailsChangedOutCodec, GroupDetailsChangedOut(guildId))

    fun sendRemovedFromRoom(
        client: ClientConnection,
        guildId: Int,
    ) = client.send(GroupDeactivatedOutCodec, GroupDeactivatedOut(guildId))

    fun sendMemberships(
        client: ClientConnection,
        parts: GuildPartCatalog,
        guilds: Collection<Guild>,
        player: Player,
    ) = client.send(
        GuildMembershipsOutCodec,
        GuildMembershipsOut(
            guilds.map {
                GuildMembershipsOut.List1Item(
                    it.id,
                    it.name,
                    it.badge,
                    parts.symbolColourHex(it),
                    parts.backgroundColourHex(it),
                    it.id == player.stats.favouriteGuild,
                    it.ownerId,
                    it.hasForum,
                )
            },
        ),
    )

    @Suppress("LongParameterList") // the member window's fields
    fun sendMembers(
        client: ClientConnection,
        guild: Guild,
        members: List<Membership>,
        page: Int,
        level: Int,
        query: String,
        viewerIsAdmin: Boolean,
        total: Int,
    ) = client.send(
        GuildMembersOutCodec,
        GuildMembersOut(
            guild.id,
            guild.name,
            guild.roomId,
            guild.badge,
            total,
            members.map { GuildMembersOut.List1Item(it.role.code, it.playerId, it.name, it.look, joinDate(it)) },
            viewerIsAdmin,
            MEMBERS_PAGE_SIZE,
            page,
            level,
            query,
        ),
    )

    fun sendMembershipRequested(
        client: ClientConnection,
        guild: Guild,
        requester: Player,
    ) = client.send(
        GroupMembershipRequestedOutCodec,
        GroupMembershipRequestedOut(
            guild.id,
            MemberLevel.APPLICANT.code,
            requester.profile.id,
            requester.profile.name,
            requester.profile.figure,
            "",
        ),
    )

    fun sendMemberUpdate(
        client: ClientConnection,
        guild: Guild,
        member: Membership,
    ) = client.send(
        GuildMembershipUpdatedOutCodec,
        GuildMembershipUpdatedOut(
            guild.id,
            member.role.code,
            member.playerId,
            member.name,
            member.look,
            joinDate(member),
        ),
    )

    fun sendMembershipRejected(
        client: ClientConnection,
        guild: Guild,
        userId: Int,
    ) = client.send(GuildMembershipRejectedOutCodec, GuildMembershipRejectedOut(guild.id, userId))

    fun sendMemberError(
        client: ClientConnection,
        guildId: Int,
        failure: GuildMemberFailure,
    ) = client.send(GuildMemberMgmtFailedOutCodec, GuildMemberMgmtFailedOut(guildId, failure.code))

    fun sendJoinFailed(
        client: ClientConnection,
        reason: GuildJoinFailure,
    ) = client.send(GroupJoinFailedOutCodec, GroupJoinFailedOut(reason.code))

    fun sendConfirmRemoveMember(
        client: ClientConnection,
        userId: Int,
        furniCount: Int,
    ) = client.send(GuildMemberFurniCountInHQOutCodec, GuildMemberFurniCountInHQOut(userId, furniCount))

    fun sendFurniWidget(
        client: ClientConnection,
        guild: Guild,
        itemId: Int,
        isMember: Boolean,
    ) = client.send(
        GuildFurniContextMenuInfoOutCodec,
        GuildFurniContextMenuInfoOut(itemId, guild.id, guild.name, guild.roomId, isMember, guild.hasForum),
    )

    fun broadcastFavourite(
        player: Player,
        guild: Guild?,
    ) {
        val rooms = player.rooms
        val playerId = player.profile.id
        rooms.cardChanged(playerId)
        val roomId = rooms.roomOf(playerId) ?: return
        val index = rooms.unitIndex(playerId) ?: return
        val update =
            if (guild == null) {
                FavoriteMembershipUpdateOut(index, NO_GROUP, NO_GROUP, "")
            } else {
                FavoriteMembershipUpdateOut(index, guild.id, FAVOURITE_STATUS, guild.name)
            }
        rooms.send(roomId, FavoriteMembershipUpdateOutCodec, update)
    }

    fun broadcastBadge(
        player: Player,
        guild: Guild,
    ) {
        val roomId = player.rooms.roomOf(player.profile.id) ?: return
        player.rooms.send(
            roomId,
            GroupBadgesOutCodec,
            GroupBadgesOut(listOf(GroupBadgesOut.List1Item(guild.id, guild.badge))),
        )
    }

    companion object {
        private const val MEMBERS_PAGE_SIZE = 14
        private const val RIGHTS_MEMBERS = 0
        private const val RIGHTS_ADMINS = 1
        private const val NO_GROUP = -1
        private const val FAVOURITE_STATUS = 3
        private const val BADGE_PARTS = 5
        private val EMPTY_PART = Triple(0, 0, 0)
        private val DEFAULT_BADGE =
            listOf(Triple(10, 3, 4), Triple(25, 17, 5), Triple(25, 17, 3), Triple(29, 11, 4), EMPTY_PART)
        private val JOIN_DATE: DateTimeFormatter
            get() = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH).withZone(GameZone.current)
        private val CREATION_DATE: DateTimeFormatter
            get() = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH).withZone(GameZone.current)

        fun joinDate(unixSeconds: Int): String = JOIN_DATE.format(Instant.ofEpochSecond(unixSeconds.toLong()))

        private fun joinDate(member: Membership): String =
            if (member.role == MemberLevel.APPLICANT) "" else joinDate(member.since)

        private fun creationDate(unixSeconds: Int): String =
            CREATION_DATE.format(Instant.ofEpochSecond(unixSeconds.toLong()))

        /** Padded to five parts. */
        private fun badgeParts(badge: String): List<Triple<Int, Int, Int>> {
            val parts = GuildBadgeCode.parse(badge).orEmpty().map { Triple(it.editorId, it.colour, it.position) }
            return parts + List((BADGE_PARTS - parts.size).coerceAtLeast(0)) { EMPTY_PART }
        }
    }
}
