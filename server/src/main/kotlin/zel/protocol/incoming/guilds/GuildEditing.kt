package zel.protocol.incoming.guilds

import zel.game.guilds.Guild
import zel.game.guilds.GuildBadgeCode
import zel.game.guilds.JoinMode

internal class GuildEditing(
    private val services: GuildServices,
    private val panel: GuildPanel,
) {
    private val guilds = services.guilds
    private val messages = services.messages

    private fun Caller.managedGroup(guildId: Int): Guild? =
        guilds.byValidId(guildId)?.takeIf { GuildRights(it, this, guilds).mayManage }

    fun rename(
        caller: Caller,
        guildId: Int,
        rawName: String,
        rawDescription: String,
    ) {
        val guild = caller.managedGroup(guildId) ?: return
        val name = services.wordFilter.filter(rawName, caller.player)
        val description = services.wordFilter.filter(rawDescription, caller.player)
        if (!GuildInput.isValidName(name) || !GuildInput.isValidDescription(description)) return
        if (guild.name == name && guild.description == description) return
        guild.show(guild.face.copy(name = name, description = description))
        guilds.save(guild)
        refreshRoom(guild)
        refreshCards(guild)
    }

    fun changeBadge(
        caller: Caller,
        guildId: Int,
        parts: List<Int>,
    ) {
        val guild = caller.managedGroup(guildId) ?: return
        val badge = GuildBadgeCode.fromEditor(parts) ?: return
        if (guild.badge.equals(badge, ignoreCase = true)) return
        guild.show(guild.face.copy(badge = badge))
        if (services.config.getBoolean("groups.badges.are_drawn")) services.badgeImager().generate(guild)
        refreshRoom(guild)
        refreshCards(guild)
        services.live().groupLookChanged(guild.roomId, guild.id)
        saveLater(guild)
    }

    fun changeColors(
        caller: Caller,
        guildId: Int,
        colorOne: Int,
        colorTwo: Int,
    ) {
        val guild = caller.managedGroup(guildId) ?: return
        if (!guilds.parts.hasSymbolColour(colorOne) || !guilds.parts.hasBackgroundColour(colorTwo)) return
        if (guild.colorOne == colorOne && guild.colorTwo == colorTwo) return
        guild.show(guild.face.copy(colorOne = colorOne, colorTwo = colorTwo))
        refreshRoom(guild)
        services.live().groupLookChanged(guild.roomId, guild.id)
        saveLater(guild)
    }

    fun changeSettings(
        caller: Caller,
        guildId: Int,
        stateValue: Int,
        rightsValue: Int,
    ) {
        val guild = caller.managedGroup(guildId) ?: return
        val mode = JoinMode.fromCode(stateValue) ?: return
        guild.admit(Guild.Joining(mode, membersDecorate = rightsValue == 0))
        refreshRoom(guild)
        saveLater(guild)
        panel.showToCaller(caller, guild)
    }

    fun deactivate(
        caller: Caller,
        guildId: Int,
    ) {
        val guild = caller.managedGroup(guildId) ?: return
        guilds.delete(guild).forEach { messages.broadcastFavourite(it, null) }
        val live = services.live()
        live.playersIn(guild.roomId).forEach { playerId ->
            services.players.find(playerId)?.client?.let { target ->
                messages.sendRemovedFromRoom(target, guild.id)
                live.sendRoomInfo(playerId, guild.roomId)
            }
        }
    }

    /** Rooms keep a card per player; the ones with this group as favourite take its new name and badge. */
    private fun refreshCards(guild: Guild) {
        services.players.all
            .filter { it.stats.favouriteGuild == guild.id }
            .forEach { it.rooms.cardChanged(it.profile.id) }
    }

    private fun saveLater(guild: Guild) {
        services.threading.run { guilds.save(guild) }
    }

    private fun refreshRoom(guild: Guild) {
        val live = services.live()
        live.playersIn(guild.roomId).forEach { playerId ->
            val online = services.players.find(playerId) ?: return@forEach
            online.client?.let { panel.show(it, guild, false, guilds.members.find(guild, online), online) }
            live.groupRightsChanged(playerId)
        }
    }
}
