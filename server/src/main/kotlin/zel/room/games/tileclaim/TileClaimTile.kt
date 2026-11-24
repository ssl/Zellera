package zel.room.games.tileclaim

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Steppable
import zel.room.games.FieldFurni
import zel.room.games.GameRun
import zel.room.games.GamesPart
import zel.room.games.StepScope
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind

class TileClaimTile(
    private val part: GamesPart,
) : FieldFurni(),
    Steppable {
    override fun onStepOn(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return
        step(StepScope(scope), furni, playerId)
    }

    fun step(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
    ) {
        val game = part.of(scope.room).tileclaim ?: return
        val member = game.memberOf(playerId) ?: return
        if (!isLocked(furni)) game.mark(scope, member, furni)
    }

    override fun onStepOff(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = Unit

    override fun onPlaced(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        val game = part.of(scope.room).tileclaim
        if (game != null && game.run != GameRun.Idle) furni.state = "1"
    }

    private fun isLocked(furni: RoomFurni): Boolean =
        (furni.state.toIntOrNull() ?: 0) % STATES_PER_COLOUR == LOCKED_STATE

    private companion object {
        const val STATES_PER_COLOUR = 3
        const val LOCKED_STATE = 2
    }
}
