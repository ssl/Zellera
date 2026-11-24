package zel.room.games.football

import zel.game.items.FurniParams
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.Usable
import zel.room.furni.behaviour.state.StateShow
import zel.room.games.FieldFurni
import zel.room.games.GamesPart
import zel.room.games.TeamColor

class FootballScoreboard(
    private val part: GamesPart,
    val color: TeamColor,
) : FieldFurni(),
    Usable {
    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        if (!part.context.hooks.hasRights(scope, playerId)) return
        when (param) {
            ADD -> change(scope, furni, 1)
            TAKE -> change(scope, furni, -1)
            else -> StateShow.whole(scope, furni, "0")
        }
        scope.raise(
            RoomEvent.StateChanged(
                furni.id,
                scope.room.units
                    .ofPlayer(playerId)
                    ?.index,
            ),
        )
    }

    companion object {
        private const val ADD = 1
        private const val TAKE = 2
        private const val MAX_SCORE = 99

        fun change(
            scope: RoomScope,
            furni: RoomFurni,
            amount: Int,
        ) {
            // The board's params are `team,highest score`.
            val highest = FurniParams.int(furni.definition.params, 1)?.takeIf { it > 0 } ?: MAX_SCORE
            val next = (furni.state.toIntOrNull() ?: 0) + amount
            val score =
                when {
                    next > highest -> 0
                    next < 0 -> highest
                    else -> next
                }
            StateShow.whole(scope, furni, score.toString())
        }
    }
}
