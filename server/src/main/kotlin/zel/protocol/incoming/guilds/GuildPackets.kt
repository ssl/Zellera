package zel.protocol.incoming.guilds

import zel.protocol.generated.AddAdminRightsToMemberInCodec
import zel.protocol.generated.ApproveAllMembershipRequestsInCodec
import zel.protocol.generated.ApproveMembershipRequestInCodec
import zel.protocol.generated.CreateGuildInCodec
import zel.protocol.generated.DeactivateGuildInCodec
import zel.protocol.generated.DeselectFavouriteGroupInCodec
import zel.protocol.generated.GetGroupBadgesInCodec
import zel.protocol.generated.GetGroupDetailsInCodec
import zel.protocol.generated.GetGuildCreationInfoInCodec
import zel.protocol.generated.GetGuildEditInfoInCodec
import zel.protocol.generated.GetGuildEditorDataInCodec
import zel.protocol.generated.GetGuildFurniContextMenuInfoInCodec
import zel.protocol.generated.GetGuildMembersInCodec
import zel.protocol.generated.GetGuildMembershipsInCodec
import zel.protocol.generated.GetMemberGuildItemCountInCodec
import zel.protocol.generated.JoinGroupInCodec
import zel.protocol.generated.KickMemberInCodec
import zel.protocol.generated.RejectMembershipRequestInCodec
import zel.protocol.generated.RemoveAdminRightsFromMemberInCodec
import zel.protocol.generated.SelectFavouriteGroupInCodec
import zel.protocol.generated.UnblockGroupMemberInCodec
import zel.protocol.generated.UpdateGuildBadgeInCodec
import zel.protocol.generated.UpdateGuildColorsInCodec
import zel.protocol.generated.UpdateGuildIdentityInCodec
import zel.protocol.generated.UpdateGuildSettingsInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute

class GuildPackets(
    services: GuildServices,
    notices: GuildMembershipNotices,
) : PacketPackage {
    private val panel = GuildPanel(services)
    private val views = GuildViews(services, panel)
    private val founding = GuildFounding(services)
    private val editing = GuildEditing(services, panel)
    private val membership = GuildMemberAdmin(services, notices, panel)
    private val favourites = GuildFavourites(services)

    override val name = "guilds"

    override val routes: List<PacketRoute<*>> =
        listOf(
            callerRoute(GetGroupBadgesInCodec) { _, _ -> },
            callerRoute(GetGuildCreationInfoInCodec) { caller, _ -> views.creationInfo(caller) },
            callerRoute(GetGuildEditorDataInCodec) { caller, _ -> views.editorData(caller) },
            callerRoute(CreateGuildInCodec, GAP) { caller, m ->
                founding.found(
                    caller,
                    GuildFounding.Request(m.name, m.description, m.roomId, m.colorOne, m.colorTwo, m.parts),
                )
            },
            callerRoute(GetGroupDetailsInCodec, GAP) { caller, m -> views.details(caller, m.guildId, m.newWindow) },
            callerRoute(GetGuildEditInfoInCodec, GAP) { caller, m -> views.editInfo(caller, m.guildId) },
            callerRoute(GetGuildMembersInCodec, GAP) { caller, m ->
                views.members(caller, m.groupId, m.pageId, m.query, m.levelId)
            },
            callerRoute(JoinGroupInCodec, GAP) { caller, m -> membership.join(caller, m.guildId) },
            callerRoute(GetGuildMembershipsInCodec, GAP) { caller, _ -> views.memberships(caller) },
            callerRoute(GetGuildFurniContextMenuInfoInCodec, GAP) { caller, m ->
                views.furniWidget(caller, m.itemId, m.guildId)
            },
            callerRoute(UpdateGuildIdentityInCodec, GAP) { caller, m ->
                editing.rename(caller, m.int1, m.string1, m.string2)
            },
            callerRoute(UpdateGuildBadgeInCodec, GAP) { caller, m -> editing.changeBadge(caller, m.guildId, m.parts) },
            callerRoute(UpdateGuildColorsInCodec, GAP) { caller, m ->
                editing.changeColors(caller, m.int1, m.int2, m.int3)
            },
            callerRoute(UpdateGuildSettingsInCodec, SLOW_GAP) { caller, m ->
                editing.changeSettings(caller, m.int1, m.int2, m.int3)
            },
            callerRoute(ApproveMembershipRequestInCodec, GAP) { caller, m ->
                membership.approve(caller, m.guildId, m.userId)
            },
            callerRoute(ApproveAllMembershipRequestsInCodec, SLOW_GAP) { caller, m ->
                membership.approveAll(caller, m.guildId)
            },
            callerRoute(RejectMembershipRequestInCodec, GAP) { caller, m ->
                membership.decline(caller, m.guildId, m.userId)
            },
            callerRoute(AddAdminRightsToMemberInCodec, GAP) { caller, m ->
                membership.setAdmin(caller, m.guildId, m.userId)
            },
            callerRoute(RemoveAdminRightsFromMemberInCodec, GAP) { caller, m ->
                membership.demoteOfficer(caller, m.guildId, m.userId)
            },
            callerRoute(KickMemberInCodec, GAP) { caller, m -> membership.removeMember(caller, m.guildId, m.userId) },
            callerRoute(
                UnblockGroupMemberInCodec,
                GAP,
            ) { caller, m -> membership.unblock(caller, m.guildId, m.userId) },
            callerRoute(GetMemberGuildItemCountInCodec, GAP) { caller, m ->
                membership.furniOfMember(caller, m.guildId, m.userId)
            },
            callerRoute(SelectFavouriteGroupInCodec, GAP) { caller, m -> favourites.select(caller, m.guildId) },
            callerRoute(DeselectFavouriteGroupInCodec, GAP) { caller, m -> favourites.clear(caller, m.guildId) },
            callerRoute(DeactivateGuildInCodec, GAP) { caller, m -> editing.deactivate(caller, m.guildId) },
        )

    private companion object {
        const val GAP = 500L
        const val SLOW_GAP = 2000L
    }
}
