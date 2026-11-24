package zel.room.quests

import zel.game.players.Player
import zel.game.quests.FurniAction
import zel.game.quests.PlayerActions
import zel.room.RoomScope
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.walk.WalkHooks

class QuestWalkHooks(
    private val players: (Int) -> Player?,
    private val offThread: (Runnable) -> Unit,
    private val next: WalkHooks = WalkHooks.None,
) : WalkHooks by next {
    override fun stepped(
        scope: RoomScope,
        unit: RoomUnit,
        onto: Point,
    ) {
        next.stepped(scope, unit, onto)
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return
        val name =
            scope.room.furni
                .at(onto)
                .topItem()
                ?.definition
                ?.name ?: return
        offThread(Runnable { PlayerActions.touchedFurni(players(playerId), FurniAction.STEP, name) })
    }
}
