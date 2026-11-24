package zel.room.furni.behaviour.state

import zel.protocol.generated.StuffData
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Toggleable
import zel.room.furni.Usable
import zel.room.furni.place.StuffOnWire
import zel.room.map.Point
import kotlin.random.Random

class MuteArea(
    private val part: StatePart,
) : Usable,
    Toggleable,
    StuffOnWire {
    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        if (!part.hooks.hasRights(scope, playerId)) return
        flip(furni)
        StateShow.data(scope, furni, furni.state)
    }

    override fun toggle(
        scope: RoomScope,
        furni: RoomFurni,
        backward: Boolean,
    ): Boolean {
        flip(furni)
        return true
    }

    override fun toggleRandom(
        scope: RoomScope,
        furni: RoomFurni,
        random: Random,
    ): Boolean = toggle(scope, furni, backward = false)

    override fun stuff(furni: RoomFurni): StuffData =
        StuffData(MAP_FORMAT, StuffData.Value.MapData(CustomValues.read(furni.state, DEFAULTS).toList()))

    fun mutes(
        furni: RoomFurni,
        tile: Point,
    ): Boolean {
        val values = CustomValues.read(furni.state, DEFAULTS)
        val anchor = furni.anchor ?: return false
        if (values["state"] != ON) return false

        fun size(key: String) = values[key]?.toIntOrNull() ?: 0
        val xs = maxOf(0, anchor.x - size("tilesBack"))..anchor.x + size("tilesFront")
        val ys = maxOf(0, anchor.y - size("tilesRight"))..anchor.y + size("tilesLeft")
        return tile.x in xs && tile.y in ys
    }

    private fun flip(furni: RoomFurni) {
        val values = CustomValues.read(furni.state, DEFAULTS)
        values["state"] = if (values["state"] == ON) OFF else ON
        StateShow.set(furni, CustomValues.write(values))
    }

    private companion object {
        const val MAP_FORMAT = 1
        const val ON = "1"
        const val OFF = "0"
        val DEFAULTS =
            linkedMapOf(
                "tilesLeft" to "0",
                "tilesRight" to "0",
                "tilesFront" to "0",
                "tilesBack" to "0",
                "state" to OFF,
            )
    }
}
