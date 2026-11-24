package zel.room.entry

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.ViewerStates
import zel.room.tick.Audience
import zel.room.tick.Lane

class EntryViewerStates(
    private val views: EntryViews = EntryViews.None,
) : ViewerStates {
    override fun resend(scope: RoomScope) {
        for (playerId in scope.room.units.playerIds()) {
            val states = EntryMessages.ConfigurationStates(views.configurationStates(scope, playerId))
            scope.out.send(Lane.Now, Audience.Player(playerId), EntryMessages.ConfigurationStatesCodec, states)
        }
    }

    override fun shows(
        scope: RoomScope,
        furni: RoomFurni,
    ): Boolean = views.shownFurni(scope, listOf(furni)).isNotEmpty()
}
