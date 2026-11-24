package zel.room.games

import zel.protocol.generated.StuffData
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Toggleable
import zel.room.furni.Usable
import zel.room.furni.behaviour.special.SpecialStuff
import zel.room.furni.place.Placeable
import zel.room.furni.place.StuffOnWire
import kotlin.random.Random

class ClockFurni(
    private val clocks: Clocks,
    private val kind: ClockKind,
) : Usable,
    StuffOnWire,
    Placeable,
    Toggleable {
    fun clock(furni: RoomFurni): GameClock = clocks.of(furni, kind)

    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) = clocks.click(scope, furni, clock(furni), playerId, param)

    override fun stuff(furni: RoomFurni): StuffData = SpecialStuff.legacy(furni, clock(furni).shown())

    override fun onPlaced(
        scope: RoomScope,
        furni: RoomFurni,
    ) = clocks.placed(scope, furni, clock(furni))

    override fun onPickedUp(
        scope: RoomScope,
        furni: RoomFurni,
    ) = clocks.pickedUp(scope, furni, clock(furni))

    override fun toggle(
        scope: RoomScope,
        furni: RoomFurni,
        backward: Boolean,
    ): Boolean {
        clocks.startFromWired(scope, furni, clock(furni))
        return false
    }

    override fun toggleRandom(
        scope: RoomScope,
        furni: RoomFurni,
        random: Random,
    ): Boolean = false
}
