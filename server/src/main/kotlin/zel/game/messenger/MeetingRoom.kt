package zel.game.messenger

import zel.room.listing.ListedRoom

/** The room a player looking for new friends is sent to. */
object MeetingRoom {
    /**
     * One of the [occupied] rooms that still has a free place, a busier room more often: each player in a room is
     * one lot. [roll] draws a number below its argument.
     */
    fun choose(
        occupied: List<ListedRoom>,
        roll: (Int) -> Int,
    ): ListedRoom? {
        val open = occupied.filter { it.userCount in 1 until it.maxPlayers }
        val lots = open.sumOf { it.userCount }
        if (lots <= 0) return null
        var drawn = roll(lots)
        return open.firstOrNull { room ->
            drawn -= room.userCount
            drawn < 0
        }
    }
}
