package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.Usable

/** Counts 0 to 99 round; -1 is switched off. Rights holders only. */
class HockeyScore(
    private val part: StatePart,
) : Usable {
    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        if (!part.hooks.hasRights(scope, playerId)) return
        val score = furni.state.toIntOrNull() ?: OFF
        val counted = score.coerceAtLeast(0)
        val next =
            when (param) {
                PLUS -> Math.floorMod(counted + 1, SCORES)
                MINUS -> Math.floorMod(counted - 1, SCORES)
                BOARD -> if (score == OFF) 0 else OFF
                else -> return
            }
        StateShow.data(scope, furni, next.toString())
        scope.raise(
            RoomEvent.StateChanged(
                furni.id,
                scope.room.units
                    .ofPlayer(playerId)
                    ?.index,
            ),
        )
    }

    private companion object {
        const val MINUS = 1
        const val PLUS = 2
        const val BOARD = 3
        const val OFF = -1
        const val SCORES = 100
    }
}
