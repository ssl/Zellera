package zel.wired.effect.user

import zel.protocol.generated.WiredFurniVisualizationStateOutCodec
import zel.protocol.generated.WiredPlayerVisualizationStateOutCodec
import zel.room.RoomScope
import zel.room.entry.EntryViews
import zel.room.tick.Audience
import zel.room.tick.Lane
import kotlin.math.abs

/** Sends a newcomer the room's kept highlights and transparency after the users. */
class VisualEntryViews(
    private val state: UserEffectState,
    private val next: EntryViews = EntryViews.None,
) : EntryViews by next {
    override fun otherUnits(
        scope: RoomScope,
        playerId: Int,
    ): Boolean {
        val others = next.otherUnits(scope, playerId)
        val to = Audience.Player(playerId)
        val kept = state.of(scope.room).visuals
        kept.keepFurni { id -> scope.room.furni[abs(id)] != null }
        kept.userState()?.let { scope.out.send(Lane.Now, to, WiredPlayerVisualizationStateOutCodec, it) }
        kept.furniState()?.let { scope.out.send(Lane.Now, to, WiredFurniVisualizationStateOutCodec, it) }
        return others
    }
}
