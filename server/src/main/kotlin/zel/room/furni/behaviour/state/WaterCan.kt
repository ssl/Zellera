package zel.room.furni.behaviour.state

import zel.game.effects.EffectRoles
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Usable

class WaterCan(
    private val part: StatePart,
) : Usable {
    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        val unit = scope.room.units.ofPlayer(playerId) ?: return
        part.hooks.giveEffect(scope, unit, EffectRoles.of(EffectRoles.WATER), scope.now() + EFFECT_MILLIS)
    }

    private companion object {
        const val EFFECT_MILLIS = 30_000L
    }
}
