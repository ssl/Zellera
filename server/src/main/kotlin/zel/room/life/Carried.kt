package zel.room.life

import zel.protocol.generated.CarryObjectOut
import zel.protocol.generated.CarryObjectOutCodec
import zel.protocol.generated.PlayerEffectOut
import zel.protocol.generated.PlayerEffectOutCodec
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit

class Carried {
    fun holdItem(
        scope: RoomScope,
        unit: RoomUnit,
        life: UnitLife,
        item: Int,
    ) {
        life.handItem = item
        if (item == 0) life.handVariable = false
        scope.out.send(Lane.Now, Audience.Room, CarryObjectOutCodec, CarryObjectOut(unit.index, item))
    }

    fun giveEffect(
        scope: RoomScope,
        unit: RoomUnit,
        life: UnitLife,
        effect: Int,
        endsAt: Long?,
    ) {
        life.effect = effect
        life.effectEndsAt = endsAt.takeIf { effect != 0 }
        scope.out.send(
            Lane.Now,
            Audience.Room,
            PlayerEffectOutCodec,
            PlayerEffectOut(unit.index, effect, 0),
        )
    }

    fun expire(
        scope: RoomScope,
        unit: RoomUnit,
        life: UnitLife,
    ) {
        val endsAt = life.effectEndsAt
        if (life.effect != 0 && endsAt != null && scope.now() >= endsAt) giveEffect(scope, unit, life, 0, null)
    }
}
