package zel.wired.effect.user

import zel.protocol.generated.WiredFurniTransparencyOut
import zel.protocol.generated.WiredFurniTransparencyOutCodec
import zel.protocol.generated.WiredPlayerTransparencyOut
import zel.protocol.generated.WiredPlayerTransparencyOutCodec
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing

class SetVisibility(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val state: UserEffectState,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val value = Transparency(at(LEVEL), at(CLICK_THROUGH) != 0, at(EASING))
        val out = VisualOut(firing, this, at(EVERYONE) != 0)
        if (at(TARGET_USERS) != 0) {
            out.units { indexes, to ->
                val m = WiredPlayerTransparencyOut(indexes, value.level, value.clickThrough, value.easing)
                firing.flash.later { it.out.send(Lane.Now, to, WiredPlayerTransparencyOutCodec, m) }
                if (to == Audience.Room) state.of(firing.scope.room).visuals.users(indexes, value)
            }
        } else {
            out.furni { ids, to ->
                val m = WiredFurniTransparencyOut(ids, value.level, value.clickThrough, value.easing)
                firing.flash.later { it.out.send(Lane.Now, to, WiredFurniTransparencyOutCodec, m) }
                if (to == Audience.Room) state.of(firing.scope.room).visuals.furni(ids, value)
            }
        }
    }

    private fun at(index: Int): Int = settings.ints.getOrNull(index) ?: 0

    companion object {
        const val EVERYONE = 0
        const val LEVEL = 1
        const val CLICK_THROUGH = 2
        const val EASING = 3
        const val TARGET_USERS = 4
    }
}
