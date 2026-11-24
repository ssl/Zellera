package zel.room.tick

import zel.room.RoomScope

class RoomTick(
    private val parts: TickParts,
) {
    fun run(
        scope: RoomScope,
        output: TickOutput,
    ) {
        output.sendLeftovers()
        parts.openFlash.run(scope)
        parts.deferredJobs.run(scope)
        parts.furni.run(scope)
        parts.wiredTimers.run(scope)
        parts.decoHosting.run(scope)
        val withPlayers =
            scope.room.units
                .playerIds()
                .isNotEmpty()
        if (withPlayers) parts.players.run(scope) else parts.emptyRoom.run(scope)
        output.sendAfterUnitUpdate()
        parts.closeFlash.run(scope)
        if (withPlayers) parts.jukebox.run(scope)
        parts.doorbell.run(scope)
        output.sendEndOfTick()
    }
}
