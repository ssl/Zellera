package zel.wired.trigger

import zel.room.RoomScope
import zel.wired.box.BoxSettings
import zel.wired.firing.Firing
import zel.wired.firing.UserSource

class BotListener(
    private val settings: BoxSettings,
    private val botName: (RoomScope, Int) -> String?,
) {
    private val bySelection = settings.userSources.firstOrNull() == UserSource.SELECTION

    fun listens(
        botIndex: Int,
        scope: RoomScope,
    ): Boolean = bySelection || (settings.text.isNotEmpty() && botName(scope, botIndex) == settings.text)

    fun answers(
        botIndex: Int?,
        firing: Firing,
    ): Boolean = !bySelection || botIndex in firing.selection.unitIndexes
}
