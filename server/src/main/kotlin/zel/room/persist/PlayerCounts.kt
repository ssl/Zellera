package zel.room.persist

import zel.room.RoomListener
import zel.room.RoomScope
import zel.room.event.RoomEvent
import java.util.concurrent.ConcurrentHashMap

/** Keeps `rooms.player_count`, the number the site shows, at the players in each loaded room. */
class PlayerCounts(
    private val save: (roomId: Int, players: Int) -> Unit,
    private val persistInOrder: (key: Any, job: Runnable) -> Unit,
) : RoomListener {
    private val written = ConcurrentHashMap<Int, Int>()

    override fun on(
        scope: RoomScope,
        event: RoomEvent,
    ) {
        if (event !is RoomEvent.Entered && event !is RoomEvent.Left) return
        val room = scope.room
        val roomId = room.info.id
        val players = room.units.playerIds().size
        if (written.put(roomId, players) == players) return
        if (players == 0) written.remove(roomId)
        persistInOrder(Key(roomId)) { save(roomId, players) }
    }

    /** One write lane per room, so the last count written is the last one counted. */
    private data class Key(
        val roomId: Int,
    )
}
