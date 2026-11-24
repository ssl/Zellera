package zel.room.trade

import zel.game.items.OwnedFurni
import zel.game.players.Player

/** Owned by the room's lock. */
class Trade(
    val first: Side,
    val second: Side,
) {
    class Side(
        val player: Player,
        val unitIndex: Int,
    ) {
        val id: Int get() = player.profile.id
        val items = LinkedHashSet<OwnedFurni>()
        var accepted = false
        var confirmed = false
    }

    val sides: List<Side> get() = listOf(first, second)

    val bothAccepted: Boolean get() = first.accepted && second.accepted

    val bothConfirmed: Boolean get() = first.confirmed && second.confirmed

    fun reopen() {
        sides.forEach {
            it.accepted = false
            it.confirmed = false
        }
    }

    fun sideOf(playerId: Int): Side? = sides.firstOrNull { it.id == playerId }

    fun holds(unitIndex: Int): Boolean = sides.any { it.unitIndex == unitIndex }
}
