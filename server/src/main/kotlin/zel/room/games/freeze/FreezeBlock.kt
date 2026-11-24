package zel.room.games.freeze

import zel.game.achievements.GameEvents
import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Steppable
import zel.room.furni.Usable
import zel.room.games.FieldFurni
import zel.room.games.GameOut
import zel.room.games.GameRun
import zel.room.games.GamesPart
import zel.room.games.StepScope
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.path.ClosedToWalks
import zel.room.unit.walk.StepGate

class FreezeBlock(
    private val part: GamesPart,
) : FieldFurni(),
    Usable,
    Steppable,
    StepGate,
    ClosedToWalks {
    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        val at = furni.anchor ?: return
        val tile =
            scope.room.furni
                .at(at)
                .furni
                .filter { it.behaviour is FreezeTile }
                .minByOrNull { it.z } ?: return
        part.of(scope.room).freeze?.throwBall(scope, playerId, tile)
    }

    override fun closed(
        room: Room,
        furni: RoomFurni,
    ): Boolean = furni.state.isEmpty() || furni.state == "0"

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
        val game = part.of(scope.room).freeze ?: return
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return
        val member = game.memberOf(playerId) as? FreezeMember ?: return
        val powerUp = (furni.state.toIntOrNull() ?: 0) / FreezeGame.POWER_UP_UNIT
        if (game.run != GameRun.Running || powerUp !in FreezeGame.FIRST_POWER_UP..FreezeGame.LAST_POWER_UP) return
        if (!game.takes(member, powerUp)) return
        furni.state = ((powerUp + TAKEN_OFFSET) * FreezeGame.POWER_UP_UNIT).toString()
        val held = StepScope(scope)
        GameOut.states(held, listOf(furni))
        game.givePowerUp(held, member, powerUp)
        part.context.hooks.progress(playerId, GameEvents.GAME_FREEZE_POWER_UP, 1)
    }

    override fun onStepOff(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = Unit

    companion object {
        private const val FIRST_BROKEN = 1
        private const val TAKEN_OFFSET = 10
        private const val STATE_LENGTH = 4

        fun closeAtEnd(block: RoomFurni): Boolean {
            val state = block.state
            if (state.length != STATE_LENGTH) return false
            val digit = state[0] - '0'
            if (digit !in FIRST_BROKEN..FreezeGame.LAST_POWER_UP) return false
            block.state = ((digit + TAKEN_OFFSET) * FreezeGame.POWER_UP_UNIT).toString()
            return true
        }
    }
}
