package zel.wired.effect.user

import zel.protocol.generated.WiredFurniHighlightOut
import zel.protocol.generated.WiredFurniHighlightOutCodec
import zel.protocol.generated.WiredPlayerHighlightOut
import zel.protocol.generated.WiredPlayerHighlightOutCodec
import zel.room.tick.Audience
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing

class SetHighlight(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val state: UserEffectState,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val ints = settings.ints
        val value = Highlight(at(TYPE), at(STRENGTH), at(COLOR), at(ANIMATION), at(EASING))
        val everyone = at(EVERYONE) != 0
        val out = VisualOut(firing, this, everyone)
        if ((ints.getOrNull(TARGET_USERS) ?: 0) != 0) {
            out.units { indexes, to ->
                val m =
                    WiredPlayerHighlightOut(
                        indexes,
                        value.type,
                        value.strength,
                        value.color,
                        value.animation,
                        value.easing,
                    )
                firing.flash.later { it.out.send(LANE, to, WiredPlayerHighlightOutCodec, m) }
                if (to == Audience.Room) state.of(firing.scope.room).visuals.users(indexes, value)
            }
        } else {
            out.furni { ids, to ->
                val m =
                    WiredFurniHighlightOut(
                        ids,
                        value.type,
                        value.strength,
                        value.color,
                        value.animation,
                        value.easing,
                    )
                firing.flash.later { it.out.send(LANE, to, WiredFurniHighlightOutCodec, m) }
                if (to == Audience.Room) state.of(firing.scope.room).visuals.furni(ids, value)
            }
        }
    }

    private fun at(index: Int): Int = settings.ints.getOrNull(index) ?: 0

    companion object {
        const val EVERYONE = 0
        const val TYPE = 1
        const val STRENGTH = 2
        const val COLOR = 3
        const val ANIMATION = 4
        const val EASING = 5
        const val TARGET_USERS = 6
        private val LANE = zel.room.tick.Lane.Now
    }
}
