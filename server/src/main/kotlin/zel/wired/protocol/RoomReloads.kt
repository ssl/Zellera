package zel.wired.protocol

import zel.protocol.generated.CloseConnectionOut
import zel.protocol.generated.CloseConnectionOutCodec
import zel.protocol.generated.RoomForwardOut
import zel.protocol.generated.RoomForwardOutCodec
import zel.protocol.generated.YouArePlayingGameOut
import zel.protocol.generated.YouArePlayingGameOutCodec
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.entry.RoomLeaving
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.wired.store.ReloadPoints
import java.util.concurrent.ConcurrentHashMap

class RoomReloads(
    private val registry: RoomRegistry,
    private val leaving: RoomLeaving,
    private val points: ReloadPoints = ReloadPoints(),
) {
    private val lastRequest = ConcurrentHashMap<Int, Long>()

    fun request(
        scope: RoomScope,
        level: Int,
        rollback: Boolean,
    ) {
        if (level < OWNER) return
        val room = scope.room
        val roomId = room.info.id
        val last = lastRequest[roomId]
        if (last != null && scope.now() - last < MIN_GAP_MILLIS) return
        lastRequest[roomId] = scope.now()
        if (!rollback) points.keep(room)
        registry.post(roomId, CLOSE_AFTER_MILLIS) { close(it, rollback) }
    }

    private fun close(
        scope: RoomScope,
        rollback: Boolean,
    ) {
        val players =
            scope.room.units
                .playerIds()
                .toList()
        // already closed here, so the unload takes them out without a second close
        leaving.doorOf(scope.room).quitting += players
        players.forEach { id ->
            val to = Audience.Player(id)
            scope.out.send(
                Lane.Now,
                to,
                YouArePlayingGameOutCodec,
                YouArePlayingGameOut(false),
            )
            scope.out.send(Lane.Now, to, CloseConnectionOutCodec, CloseConnectionOut)
        }
        registry.post(scope.room.info.id, FORWARD_AFTER_MILLIS) { forward(it, players, rollback) }
    }

    private fun forward(
        scope: RoomScope,
        players: List<Int>,
        rollback: Boolean,
    ) {
        val roomId = scope.room.info.id
        if (rollback) points.restore(scope.room)
        points.reloading(roomId)
        registry.unload(roomId)
        players.forEach { id ->
            scope.out.send(
                Lane.Now,
                Audience.Player(id),
                RoomForwardOutCodec,
                RoomForwardOut(roomId),
            )
        }
    }

    private companion object {
        const val OWNER = 4
        const val MIN_GAP_MILLIS = 5000L
        const val CLOSE_AFTER_MILLIS = 300L
        const val FORWARD_AFTER_MILLIS = 2000L
    }
}
