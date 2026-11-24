package zel.room.furni

import zel.protocol.generated.ObjectRemoveMultipleOut
import zel.protocol.generated.ObjectRemoveMultipleOutCodec
import zel.protocol.generated.ObjectsOutCodec
import zel.room.RoomScope
import zel.room.entry.EntryMessages
import zel.room.entry.EntryViews
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.tick.TickPart

/** Each tick: the furni that came near a player are sent, those left behind taken away. */
class FurniSightPart(
    private val views: EntryViews,
) : TickPart {
    override fun run(scope: RoomScope) {
        val sight = scope.room.sight
        if (!sight.active) return
        val players = scope.room.units.playerIds()
        (sight.players() - players.toSet()).forEach(sight::forget)
        for (playerId in players) {
            val changes = sight.changes(playerId)
            val to = Audience.Player(playerId)
            if (changes.leaves.isNotEmpty()) {
                val gone = ObjectRemoveMultipleOut(changes.leaves, NO_PICKER, true)
                scope.out.send(Lane.Now, to, ObjectRemoveMultipleOutCodec, gone)
            }
            if (changes.enters.isEmpty()) continue
            val shown = views.shownFurni(scope, changes.enters)
            if (shown.isNotEmpty()) scope.out.send(Lane.Now, to, ObjectsOutCodec, EntryMessages.objects(shown))
        }
    }

    private companion object {
        const val NO_PICKER = -1
    }
}
