package zel.game.moderation

import zel.game.permissions.Permissions
import zel.game.players.Player
import zel.game.players.PlayerProfile
import zel.game.players.Players

/** Whom a staff member may act on or look into: players they outrank, never themselves. */
class StaffReach(
    private val players: Players,
    private val permissions: Permissions,
) {
    fun canModerate(
        moderator: Player?,
        targetId: Int,
    ): Boolean {
        if (moderator == null || targetId <= 0 || targetId == moderator.profile.id) return false
        return reaches(moderator, players.profileOf(targetId))
    }

    fun reaches(
        moderator: Player,
        target: PlayerProfile?,
    ): Boolean =
        target != null &&
            target.id != moderator.profile.id &&
            permissions.outranks(moderator.profile.rank.id, target.rank.id)
}
