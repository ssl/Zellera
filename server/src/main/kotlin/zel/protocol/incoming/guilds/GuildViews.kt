package zel.protocol.incoming.guilds

import zel.game.guilds.Guild
import zel.game.guilds.GuildDirectory
import zel.game.guilds.MemberLevel
import zel.game.permissions.isAnyGroupOwner
import zel.game.players.Player
import zel.protocol.typed.PacketIds

internal class GuildViews(
    private val services: GuildServices,
    private val panel: GuildPanel,
) {
    private val guilds = services.guilds
    private val messages = services.messages

    fun creationInfo(caller: Caller) {
        val free = roomsWithoutGroup(caller.userId)
        messages.sendCreationInfo(caller.client, services.config.getInt("groups.founding.price.credits"), free)
    }

    private fun roomsWithoutGroup(ownerId: Int) = services.rooms.ofOwner(ownerId).filter { it.guildId == 0 }

    fun editorData(caller: Caller) = messages.sendEditorData(caller.client, guilds.parts)

    fun details(
        caller: Caller,
        guildId: Int,
        newWindow: Boolean,
    ) {
        val guild = guilds.byValidId(guildId) ?: return
        panel.show(caller.client, guild, newWindow, guilds.members.find(guild, caller.player), caller.player)
    }

    fun editInfo(
        caller: Caller,
        guildId: Int,
    ) {
        val guild = guilds.byValidId(guildId) ?: return
        val isStaff = caller.player.isAnyGroupOwner
        val rank = guilds.members.find(guild, caller.player)?.role
        if (isStaff || caller.userId == guild.ownerId || rank == MemberLevel.OFFICER) {
            messages.sendEditInfo(caller.client, guild, roomsWithoutGroup(guild.ownerId))
        }
    }

    fun members(
        caller: Caller,
        groupId: Int,
        pageId: Int,
        query: String,
        tab: Int,
    ) {
        val tabKnown = tab in 0..REQUESTS_TAB || tab == BLOCKED_TAB
        if (pageId !in 0..MAX_PAGE_ID || !tabKnown || query.length > MAX_QUERY_LENGTH) return
        val guild = guilds.byValidId(groupId) ?: return
        val admin =
            caller.player.isAnyGroupOwner ||
                (
                    (guild.id in caller.player.stats.guildIds) &&
                        guilds.members
                            .find(guild, caller.player)
                            ?.role
                            ?.isAdmin == true
                )
        val shownTab = if (tab >= REQUESTS_TAB && !admin) 0 else tab
        messages.sendMembers(
            caller.client,
            guild,
            guilds.members.page(guild, pageId, shownTab, query),
            pageId,
            shownTab,
            query,
            admin,
            guilds.members.count(guild, shownTab, query),
        )
    }

    fun memberships(caller: Caller) =
        messages.sendMemberships(caller.client, guilds.parts, groupsOf(caller.player, guilds), caller.player)

    fun furniWidget(
        caller: Caller,
        itemId: Int,
        guildId: Int,
    ) {
        if (!GuildInput.areIds(itemId, guildId)) return
        val roomId = caller.player.rooms.roomOf(caller.userId) ?: return
        val item = caller.player.rooms.furniIn(roomId, itemId)
        val guild = guilds.byId(guildId)
        if (item != null && guild != null) {
            val isMember = guilds.members.find(guild, caller.player) != null
            messages.sendFurniWidget(caller.client, guild, item.id, isMember)
        }
    }

    private companion object {
        const val MAX_PAGE_ID = 1000
        const val MAX_QUERY_LENGTH = 32
        const val REQUESTS_TAB = 2
        const val BLOCKED_TAB = 3
    }
}

internal fun GuildDirectory.byValidId(guildId: Int): Guild? = if (PacketIds.isId(guildId)) byId(guildId) else null

internal fun groupsOf(
    player: Player,
    guilds: GuildDirectory,
): Set<Guild> =
    player.stats.guildIds
        .filter { it != 0 }
        .mapNotNullTo(LinkedHashSet()) { guilds.byId(it) }
