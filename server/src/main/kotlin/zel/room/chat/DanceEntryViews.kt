package zel.room.chat

import zel.protocol.generated.DanceOut
import zel.protocol.generated.DanceOutCodec
import zel.room.RoomScope
import zel.room.entry.EntryViews
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit

class DanceEntryViews(
    private val dances: Dances,
    private val next: EntryViews = EntryViews.None,
) : EntryViews by next {
    override fun afterBurst(
        scope: RoomScope,
        unit: RoomUnit,
        playerId: Int,
    ) {
        next.afterBurst(scope, unit, playerId)
        for (dancer in scope.room.units.all()) {
            val dance = dances.danceOf(dancer)
            if (dance == 0) continue
            val message = DanceOut(dancer.index, dance)
            scope.out.send(Lane.Now, Audience.Player(playerId), DanceOutCodec, message)
        }
    }
}
