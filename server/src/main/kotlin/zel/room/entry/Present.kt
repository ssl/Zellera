package zel.room.entry

import zel.game.players.Player

data class Present(
    val unitIndex: Int,
    val level: Int,
    val card: PlayerCard,
    val session: Player? = null,
    /** Watching without an avatar; [unitIndex] is [NO_UNIT]. */
    val spectator: Boolean = false,
    /** The account level at the entry, which the visit's hosting points count with; [card] keeps the current one. */
    val enteredLevel: Int = card.accountLevel,
) {
    companion object {
        const val NO_UNIT = -1
    }
}
