package zel.wired.effect.furni

import zel.wired.condition.MoveProbe
import zel.wired.firing.Firing

/** An effect that needs a user and has none is left out. */
class FurniMoveProbe(
    private val moves: FurniMoves,
) : MoveProbe {
    override fun completes(
        firing: Firing,
        conditionId: Int,
    ): Boolean {
        val trial = MoveTrial(moves, firing)
        try {
            firing.effects
                .filter { !it.needsUser || firing.triggeringUnit != null }
                .filterIsInstance<TriesMoves>()
                .forEach { it.tryMoves(firing, trial) }
            return !trial.refused
        } finally {
            trial.undo()
        }
    }
}
