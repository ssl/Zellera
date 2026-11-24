package zel.room.furni.behaviour.special

import zel.game.items.StateFields
import zel.protocol.generated.StuffData
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Toggleable
import zel.room.furni.Usable
import zel.room.furni.behaviour.state.StateHooks
import zel.room.furni.behaviour.state.StateShow
import zel.room.furni.place.StuffOnWire
import kotlin.random.Random

class BackgroundToner(
    private val hooks: StateHooks,
) : Usable,
    Toggleable,
    StuffOnWire {
    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        if (!hooks.hasRights(scope, playerId)) return
        StateShow.whole(scope, furni, flipped(furni.state))
    }

    override fun toggle(
        scope: RoomScope,
        furni: RoomFurni,
        backward: Boolean,
    ): Boolean {
        StateShow.set(furni, flipped(furni.state))
        return true
    }

    override fun toggleRandom(
        scope: RoomScope,
        furni: RoomFurni,
        random: Random,
    ): Boolean = toggle(scope, furni, backward = false)

    override fun stuff(furni: RoomFurni): StuffData =
        SpecialStuff.of(furni, SpecialStuff.INTS, StuffData.Value.IntArray(values(furni.state)))

    fun setColour(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        colour: List<Int>,
    ) {
        if (!hooks.hasRights(scope, playerId)) return
        val on = values(furni.state).first()
        StateShow.whole(scope, furni, state(listOf(on) + colour))
    }

    companion object {
        private const val MAX_LEVEL = 255
        private const val ON = "on"
        private val LEVELS = listOf("hue", "saturation", "lightness")

        /** The tone of a toner that was never given a colour. */
        private val UNTONED = listOf(0, 0, 255)

        /** Switched on (1 or 0), hue, saturation, lightness: the order the client reads them in. */
        fun values(state: String): List<Int> {
            val fields = StateFields.read(state) ?: return listOf(0) + UNTONED
            return listOf(if (fields.flag(ON)) 1 else 0) + LEVELS.map { fields.number(it).coerceIn(0, MAX_LEVEL) }
        }

        fun state(values: List<Int>): String {
            val writer = StateFields.write().flag(ON, values.firstOrNull() == 1)
            LEVELS.forEachIndexed { index, name ->
                writer.number(name, (values.getOrNull(index + 1) ?: UNTONED[index]).coerceIn(0, MAX_LEVEL))
            }
            return writer.state()
        }

        fun flipped(state: String): String {
            val values = values(state)
            return state(listOf(1 - values[0]) + values.drop(1))
        }
    }
}
