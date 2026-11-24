package zel.game.players

import zel.db.SqlFailure
import zel.game.permissions.PermissionActor
import zel.game.permissions.Privilege
import zel.game.permissions.Rank
import zel.game.permissions.applyRankItems
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.users.UserPerks
import zel.protocol.outgoing.users.UserPermissions
import zel.server.GameServices

internal class PlayerRanks(
    private val services: GameServices,
    private val players: Players,
) {
    @Throws(SqlFailure::class)
    fun change(
        userId: Int,
        rankId: Int,
        actorId: Int = PermissionActor.SYSTEM_ID,
        actorName: String = "system",
        via: String = "system",
    ) {
        val permissions = services.game.permissions.required()
        val rank = requireNotNull(permissions.ranks[rankId]) { "no rank $rankId" }
        val online = players.find(userId)
        val before = online?.profile?.rank?.id ?: storedRank(userId)
        services.database.queries.write("UPDATE players SET rank_id = ? WHERE id = ?", rankId, userId)
        permissions.rankLog.write(PermissionActor(actorId, actorName, via), userId, before, rankId)
        online?.follow(rank)
    }

    private fun Player.follow(rank: Rank) {
        val oldBadge = profile.rank.badge
        profile.rank = rank
        if (oldBadge.isNotEmpty() && oldBadge != rank.badge) {
            inventory.badges.find(oldBadge)?.let(inventory.badges::discard)
        }
        applyRankItems(this)
        client?.send(UserPermissions(this))
        client?.send(UserPerks(this))
        rooms.refreshRights(profile.id)
        if (hasPermission(Privilege.MODERATION_TOOL)) services.game.moderationService?.sendModeratorInit(this)
        client?.let(CatalogReplies::catalogUpdated)
    }

    private fun storedRank(userId: Int): Int =
        services.database.queries
            .select("SELECT rank_id FROM players WHERE id = ?", userId)
            .first { it.getInt("rank_id") } ?: NO_RANK

    private companion object {
        const val NO_RANK = -1
    }
}
