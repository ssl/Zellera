package zel.room.games

import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Steppable
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.path.ClosedToWalks
import zel.room.unit.walk.StepGate

class FieldGate(
    private val part: GamesPart,
    val family: FieldFamily,
    val color: TeamColor,
) : FieldFurni(),
    StepGate,
    ClosedToWalks,
    Steppable {
    override fun closed(
        room: Room,
        furni: RoomFurni,
    ): Boolean {
        val games = part.of(room)
        val game = if (family == FieldFamily.Freeze) games.freeze else games.tileclaim
        return game != null && game.run != GameRun.Idle
    }

    override fun lets(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ): Boolean = !closed(scope.room, furni)

    override fun refused(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = Unit

    override fun onStepOn(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return
        val games = part.of(scope.room)
        val game: FieldGame = if (family == FieldFamily.Freeze) games.freezeOrNew() else games.tileclaimOrNew()
        val member = game.memberOf(playerId)
        val held = StepScope(scope)
        if (member != null) game.leave(held, member) else game.join(held, unit, color)
    }

    override fun onStepOff(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = Unit
}
