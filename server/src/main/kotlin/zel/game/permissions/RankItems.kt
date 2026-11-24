package zel.game.permissions

import zel.game.effects.EffectRoles
import zel.game.players.Player
import zel.game.players.inventory.AvatarEffect

/** Settles a level's items: rank badge, the VIP badge from level 2 (kept), the Ambassador effect at level 3 only. */
fun applyRankItems(player: Player) {
    val level = player.profile.rank.id
    val perks = player.inventory

    val wantedBadges = listOf(player.profile.rank.badge, SecurityLevel.VIP_BADGE.takeIf { level == SecurityLevel.VIP })
    wantedBadges
        .filterNotNull()
        .filter { it.isNotEmpty() && !perks.badges.owns(it) }
        .forEach { perks.badges.award(it) }

    val ambassador = EffectRoles.of(EffectRoles.AMBASSADOR)
    if (ambassador == 0) return
    when {
        level != SecurityLevel.AMBASSADOR -> if (perks.effects.owns(ambassador)) perks.effects.remove(ambassador)
        !perks.effects.ownsPermanent(ambassador) -> perks.effects.grant(ambassador, AvatarEffect.PERMANENT)
    }
}
