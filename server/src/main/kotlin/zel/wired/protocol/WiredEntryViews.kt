package zel.wired.protocol

import zel.protocol.generated.NotificationDialogOut
import zel.protocol.generated.WiredEnvironmentOut
import zel.room.RoomScope
import zel.room.entry.DoorCheck
import zel.room.entry.EntryViews
import zel.wired.addon.AchievementEnvironment
import zel.wired.store.RoomBoxForms
import zel.wired.trigger.TextCaptureNotice

class WiredEntryViews(
    private val forms: RoomBoxForms,
    private val next: EntryViews = EntryViews.None,
) : EntryViews by next {
    private val environment = AchievementEnvironment(forms)

    override fun wiredEnvironment(scope: RoomScope): WiredEnvironmentOut = environment.message(scope)

    /** Owner only, as the room's other wired errors. */
    override fun entryNotices(
        scope: RoomScope,
        level: Int,
    ): List<NotificationDialogOut> {
        val own = next.entryNotices(scope, level)
        if (level < DoorCheck.OWNER || !TextCaptureNotice.applies(scope.room, forms)) return own
        return own + TextCaptureNotice.dialog()
    }

    override fun wiredPermissions(
        scope: RoomScope,
        level: Int,
    ): Pair<Boolean, Boolean> = forms.masks(scope.room).let { it.mayModify(level) to it.mayRead(level) }
}
