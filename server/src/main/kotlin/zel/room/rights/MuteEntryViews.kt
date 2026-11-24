package zel.room.rights

import zel.room.RoomScope
import zel.room.entry.EntryViews

class MuteEntryViews(
    private val moderation: Moderation,
    private val next: EntryViews = EntryViews.None,
) : EntryViews by next {
    override fun allMuted(scope: RoomScope): Boolean = moderation.allMuted(scope)
}
