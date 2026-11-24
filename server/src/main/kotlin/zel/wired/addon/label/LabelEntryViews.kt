package zel.wired.addon.label

import zel.protocol.generated.FurnitureLabelsAndRegionsOut
import zel.room.RoomScope
import zel.room.entry.EntryViews

class LabelEntryViews(
    private val labels: RoomLabels,
    private val out: LabelOut,
    private val next: EntryViews = EntryViews.None,
) : EntryViews by next {
    override fun labels(
        scope: RoomScope,
        level: Int,
    ): FurnitureLabelsAndRegionsOut = out.entries(scope, labels.all(scope.room), level)
}
