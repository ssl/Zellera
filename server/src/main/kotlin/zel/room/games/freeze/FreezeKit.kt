package zel.room.games.freeze

import zel.game.effects.EffectRoles
import zel.game.items.FurniParams
import zel.room.games.TeamColor

/**
 * What a Freeze player wears and starts with. The team's gate names the team effect (`team,effect`), the field's
 * tiles the rest (`frozen effect per team:shielded effect per team:...:lives`); the numbers here stand in for
 * furni that name none.
 */
data class FreezeKit(
    val teamEffect: Int,
    val shieldedEffect: Int,
    val frozenEffect: Int,
    val lives: Int,
) {
    companion object {
        private const val START_LIVES = 3
        private const val FROZEN_GROUP = 0
        private const val SHIELDED_GROUP = 1
        private const val LIVES_GROUP = 4

        fun standard(color: TeamColor) =
            FreezeKit(
                teamEffect = EffectRoles.of(EffectRoles.FREEZE_TEAM + color.name.lowercase()),
                shieldedEffect = EffectRoles.of(EffectRoles.FREEZE_SHIELD + color.name.lowercase()),
                frozenEffect = EffectRoles.of(EffectRoles.FROZEN),
                lives = START_LIVES,
            )

        fun of(
            color: TeamColor,
            gateEffect: Int?,
            tileParams: String?,
        ): FreezeKit {
            val standard = standard(color)
            val groups = tileParams?.let(FurniParams::groups).orEmpty()
            val ofTeam = { group: Int -> groups.getOrNull(group)?.let { FurniParams.int(it, color.code - 1) } }
            return FreezeKit(
                teamEffect = gateEffect ?: standard.teamEffect,
                shieldedEffect = ofTeam(SHIELDED_GROUP)?.takeIf { it > 0 } ?: standard.shieldedEffect,
                frozenEffect = ofTeam(FROZEN_GROUP)?.takeIf { it > 0 } ?: standard.frozenEffect,
                lives = groups.getOrNull(LIVES_GROUP)?.toIntOrNull()?.takeIf { it > 0 } ?: standard.lives,
            )
        }
    }
}
