package zel.room.quests

import zel.game.players.Player
import zel.game.quests.CountedAction
import zel.game.quests.FurniAction
import zel.game.quests.PlayerActions
import zel.room.RoomListener
import zel.room.RoomScope
import zel.room.event.PlayerAction
import zel.room.event.RoomEvent
import zel.room.unit.UnitKind

class QuestListener(
    private val players: (Int) -> Player?,
) : RoomListener {
    override fun on(
        scope: RoomScope,
        event: RoomEvent,
    ) {
        when (event) {
            is RoomEvent.ClickFurni -> {
                val name =
                    scope.room.furni[event.furniId]
                        ?.definition
                        ?.name ?: return
                PlayerActions.touchedFurni(player(scope, event.unitIndex), FurniAction.CLICK, name)
            }

            is RoomEvent.StateChanged -> {
                count(scope, event.unitIndex, CountedAction.SWITCH_ITEM_STATE)
            }

            is RoomEvent.Action -> {
                when (event.action) {
                    PlayerAction.WAVE -> count(scope, event.unitIndex, CountedAction.WAVE)
                    PlayerAction.DANCE -> count(scope, event.unitIndex, CountedAction.DANCE)
                }
            }

            else -> {}
        }
    }

    private fun count(
        scope: RoomScope,
        unitIndex: Int?,
        goal: CountedAction,
    ) {
        val player = unitIndex?.let { player(scope, it) } ?: return
        PlayerActions.progress(player, goal, 1)
    }

    private fun player(
        scope: RoomScope,
        unitIndex: Int,
    ): Player? = (scope.room.units[unitIndex]?.kind as? UnitKind.Player)?.let { players(it.playerId) }
}
