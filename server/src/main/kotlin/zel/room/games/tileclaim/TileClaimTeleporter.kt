package zel.room.games.tileclaim

import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.Steppable
import zel.room.furni.behaviour.state.StateShow
import zel.room.games.FieldFurni
import zel.room.games.GameOut
import zel.room.games.GamesPart
import zel.room.games.StepScope
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection

class TileClaimTeleporter(
    private val part: GamesPart,
) : FieldFurni(),
    Steppable {
    override fun onStepOn(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        val context = part.context
        val others =
            scope.room.furni
                .all()
                .filter { it.behaviour is TileClaimTeleporter && it !== furni }
        val target = others.randomOrNull(context.random) ?: return
        furni.state = LIT
        GameOut.states(StepScope(scope), listOf(furni))
        part.hold(unit)
        unit.path = emptyList()
        val room = scope.room
        context.jobs.after(room, TARGET_LIGHT_MS) { StateShow.data(it, target, LIT) }
        context.jobs.after(room, LAND_MS) { land(it, furni, target, unit) }
        context.jobs.after(room, SOURCE_DARK_MS) { StateShow.data(it, furni, DARK) }
        context.jobs.after(room, TARGET_DARK_MS) { StateShow.data(it, target, DARK) }
    }

    override fun onStepOff(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = Unit

    private fun land(
        scope: RoomScope,
        from: RoomFurni,
        target: RoomFurni,
        unit: RoomUnit,
    ) {
        part.release(unit)
        val at = target.anchor
        if (scope.room.units[unit.index] !== unit || at == null) return
        val context = part.context
        scope.raise(RoomEvent.StepOff(unit.index, from.id))
        context.walking.place(scope, unit, at)
        unit.face(context.random.nextInt(UnitDirection.COUNT))
        scope.raise(RoomEvent.StepOn(unit.index, target.id))
        context.walking.sendNow(scope, listOf(unit))
    }

    private companion object {
        const val LIT = "1"
        const val DARK = "0"
        const val TARGET_LIGHT_MS = 500L
        const val LAND_MS = 750L
        const val SOURCE_DARK_MS = 1000L
        const val TARGET_DARK_MS = 1250L
    }
}
