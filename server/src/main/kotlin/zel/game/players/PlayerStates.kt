package zel.game.players

import java.util.concurrent.ConcurrentHashMap

internal class PlayerStates<S : PlayerStates.Held>(
    private val saves: PlayerSaveQueue,
    private val isCurrent: (S) -> Boolean,
    private val load: (playerId: Int, outdated: S?) -> S,
) {
    /** A state a change must leave alone once [dropped]: the next change goes to the state [of] hands out then. */
    open class Held {
        @Volatile
        var dropped = false
    }

    private val held = ConcurrentHashMap<Int, S>()

    fun of(playerId: Int): S {
        held[playerId]?.takeIf(isCurrent)?.let { return it }
        // one load per player: two threads loading at once would each change their own copy
        return checkNotNull(
            held.compute(playerId) { id, cached ->
                if (cached != null && isCurrent(cached)) cached else load(id, cached)
            },
        )
    }

    fun <T> change(
        playerId: Int,
        change: (S) -> T,
    ): T {
        while (true) {
            val state = of(playerId)
            synchronized(state) {
                if (!state.dropped) return change(state)
            }
        }
    }

    fun write(
        playerId: Int,
        write: () -> Unit,
    ) = saves.submit(playerId, write)

    /** The state goes once its writes are in the database, so a relog never loads a paid reward as unpaid. */
    fun unload(playerId: Int) {
        held[playerId]?.let { dropOnceSaved(playerId, it) }
    }

    private fun dropOnceSaved(
        playerId: Int,
        state: S,
    ) {
        saves.whenIdle(playerId) {
            synchronized(state) {
                if (saves.isIdle(playerId)) {
                    state.dropped = true
                    held.remove(playerId, state)
                } else {
                    dropOnceSaved(playerId, state)
                }
            }
        }
    }
}
