package zel.room.furni.behaviour.state

import zel.protocol.generated.StuffData
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Steppable
import zel.room.furni.behaviour.special.ObjectDataEditable
import zel.room.furni.behaviour.special.SpecialStuff
import zel.room.furni.place.StuffOnWire
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind

class EffectProviderTile(
    private val part: StatePart,
) : Steppable,
    ObjectDataEditable,
    StuffOnWire {
    override fun stuff(furni: RoomFurni): StuffData = SpecialStuff.map(furni, values(furni))

    override fun edit(
        scope: RoomScope,
        furni: RoomFurni,
        values: Map<String, String>,
    ) {
        val effect = values[EFFECT]?.toIntOrNull() ?: return
        if (part.hooks.blocksEffect(scope.room.info.ownerId, effect)) return
        val current = values(furni)
        current[EFFECT] = effect.toString()
        StateShow.whole(scope, furni, CustomValues.write(current))
    }

    override fun onStepOn(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        val effect = values(furni)[EFFECT]?.toIntOrNull() ?: 0
        flash(scope, furni, LIT)
        part.later(scope, furni, LIT_MILLIS) { later, tile -> flash(later, tile, DARK) }
        val given = if (part.hooks.effect(unit) == effect) 0 else effect
        val player = unit.kind as? UnitKind.Player
        if (given != 0 && player != null && part.hooks.blocksEffect(player.playerId, given)) return
        part.hooks.giveEffect(scope, unit, given, null)
    }

    override fun onStepOff(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = Unit

    private fun flash(
        scope: RoomScope,
        furni: RoomFurni,
        state: String,
    ) {
        val values = values(furni)
        values[STATE] = state
        StateShow.whole(scope, furni, CustomValues.write(values))
    }

    private fun values(furni: RoomFurni) = CustomValues.read(furni.state, mapOf(EFFECT to "0"))

    private companion object {
        const val EFFECT = "effectId"
        const val STATE = "state"
        const val LIT = "1"
        const val DARK = "0"
        const val LIT_MILLIS = 500L
    }
}
