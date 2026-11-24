package zel.room.life

import zel.protocol.generated.PlayerEffectOut
import zel.protocol.generated.PlayerEffectOutCodec
import zel.room.RoomScope
import zel.room.entry.EntryViews
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit

/**
 * The effects the room's units wear. After its burst a newcomer gets one AvatarEffect per other unit that wears one,
 * before the dances; a unit without an effect gets none.
 */
class EffectEntryViews(
    private val life: RoomLife,
    private val next: EntryViews = EntryViews.None,
) : EntryViews by next {
    override fun effect(unit: RoomUnit): Int = life.effect(unit)

    override fun afterBurst(
        scope: RoomScope,
        unit: RoomUnit,
        playerId: Int,
    ) {
        for (other in scope.room.units.all()) {
            if (other === unit) continue
            val effect = life.effect(other)
            if (effect == 0) continue
            val message = PlayerEffectOut(other.index, effect, 0)
            scope.out.send(Lane.Now, Audience.Player(playerId), PlayerEffectOutCodec, message)
        }
        next.afterBurst(scope, unit, playerId)
    }
}
