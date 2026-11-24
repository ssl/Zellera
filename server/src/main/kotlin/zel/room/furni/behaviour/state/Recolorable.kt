package zel.room.furni.behaviour.state

import zel.game.items.RecolorablePalettes
import zel.protocol.generated.ObjectUpdateOutCodec
import zel.protocol.generated.StuffData
import zel.room.RoomScope
import zel.room.furni.HeightMapChange
import zel.room.furni.RoomFurni
import zel.room.furni.place.FurniOut
import zel.room.furni.place.FurniWire
import zel.room.furni.place.StuffOnWire
import zel.room.tick.Audience
import zel.room.tick.Lane
import java.util.Locale
import kotlin.random.Random

class Recolorable(
    part: StatePart,
    private val palettes: () -> RecolorablePalettes = RecolorablePalettes::get,
) : Multistate(part),
    StuffOnWire {
    override fun clicked(
        scope: RoomScope,
        furni: RoomFurni,
    ): String? =
        if (heldDown(scope, furni)) {
            null
        } else {
            withState(furni, stepped(state(furni), furni.definition.stateCount, false))
        }

    override fun show(
        scope: RoomScope,
        furni: RoomFurni,
        state: String,
    ) {
        val change = HeightMapChange.of(scope.room, furni.tiles())
        StateShow.data(scope, furni, state)
        if (stepsDown(furni)) {
            scope.out.send(Lane.Now, Audience.Room, ObjectUpdateOutCodec, FurniWire.objectUpdate(furni))
        }
        FurniOut.heights(scope, change)
        part.hooks.heightsChanged(scope, furni.tiles())
    }

    override fun toggle(
        scope: RoomScope,
        furni: RoomFurni,
        backward: Boolean,
    ): Boolean {
        if (heldDown(scope, furni)) return false
        val next = withState(furni, stepped(state(furni), furni.definition.stateCount, backward)) ?: return false
        StateShow.set(furni, next)
        return true
    }

    override fun toggleRandom(
        scope: RoomScope,
        furni: RoomFurni,
        random: Random,
    ): Boolean {
        if (heldDown(scope, furni)) return false
        val states = furni.definition.stateCount
        StateShow.set(
            furni,
            withState(furni, (if (states > 0) random.nextInt(states) else 0).toString()) ?: return false,
        )
        return true
    }

    override fun stuff(furni: RoomFurni): StuffData {
        val rgb = color(furni)
        return StuffData(
            MAP_FORMAT,
            StuffData.Value.MapData(
                listOf(
                    "r" to ((rgb shr RED) and CHANNEL).toString(),
                    "b" to (rgb and CHANNEL).toString(),
                    "g" to ((rgb shr GREEN) and CHANNEL).toString(),
                    "state" to state(furni),
                ),
            ),
        )
    }

    fun color(furni: RoomFurni): Int =
        furni.state
            .substringAfter(SEPARATOR, "")
            .toIntOrNull(HEX)
            ?.and(RGB) ?: palettes().defaultColor(furni.definition.name)

    fun state(furni: RoomFurni): String = furni.state.substringBefore(SEPARATOR).ifEmpty { "0" }

    fun stored(
        state: String,
        rgb: Int,
    ): String = state + SEPARATOR + "%06X".format(Locale.ROOT, rgb and RGB)

    fun heldDown(
        scope: RoomScope,
        furni: RoomFurni,
    ): Boolean {
        if ((
                furni.definition.params
                    .trim()
                    .toDoubleOrNull() ?: 0.0
            ) >= 0.0
        ) {
            return false
        }
        return furni.tiles().any { tile ->
            val top =
                scope.room.furni
                    .at(tile)
                    .topItem()
            top != null && top !== furni && top.z >= furni.z
        }
    }

    private fun stepsDown(furni: RoomFurni): Boolean =
        (
            furni.definition.params
                .trim()
                .toDoubleOrNull() ?: 0.0
        ) < 0.0

    private fun withState(
        furni: RoomFurni,
        state: String?,
    ): String? = state?.let { stored(it, color(furni)) }

    private companion object {
        const val SEPARATOR = ';'
        const val MAP_FORMAT = 1
        const val HEX = 16
        const val RGB = 0xFFFFFF
        const val CHANNEL = 0xFF
        const val RED = 16
        const val GREEN = 8
    }
}
