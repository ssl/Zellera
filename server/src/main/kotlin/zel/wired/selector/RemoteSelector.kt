package zel.wired.selector

import zel.wired.WiredRooms
import zel.wired.box.BoxSettings
import zel.wired.box.SelectorBox
import zel.wired.firing.Firing
import zel.wired.firing.Selection
import kotlin.math.absoluteValue

class RemoteSelector(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val rooms: () -> WiredRooms?,
) : SelectorBox {
    override fun select(
        firing: Firing,
        selection: Selection,
    ) {
        val room = firing.scope.room
        val picked = settings.picks.mapNotNull { room.furni[it.absoluteValue] }
        val remote = remoteBoxes(firing, picked.map { it.id }.toSet())
        val joined = Selection()
        remote.forEach { it.select(firing, joined) }
        val furni = remote.isEmpty() || remote.any { it is FurniSelector }
        val units = remote.isEmpty() || remote.any { it is UnitSelector }
        if (furni) {
            SelectionMerge.apply(
                selection.furniIds,
                settings,
                joined.furniIds.filterTo(LinkedHashSet()) { id ->
                    room.furni[id]?.let { Candidates.selectable(it, true, anyBox = true) } ==
                        true
                },
                { Candidates.furni(room, true) },
            )
        }
        if (units) {
            SelectionMerge.apply(
                selection.unitIndexes,
                settings,
                joined.unitIndexes,
                { Candidates.units(room, false) },
            )
        }
    }

    private fun remoteBoxes(
        firing: Firing,
        ids: Set<Int>,
    ): List<SelectorBox> =
        rooms()
            ?.selectors(firing.scope)
            .orEmpty()
            .filter { it.furniId in ids && it !is RemoteSelector }
}
