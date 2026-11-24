package zel.wired.selector

import zel.wired.box.SelectorBox
import zel.wired.firing.Firing
import zel.wired.firing.Selection

abstract class FurniSelector : SelectorBox {
    protected open val wallItems: Boolean get() = false

    /** Whether every wired box kind is selectable, not only add-ons (Candidates.selectable). */
    protected open val anyBox: Boolean get() = true

    protected open fun unconfigured(firing: Firing): Boolean = false

    /** The furni ids this box matches, before the filter and invert flags. */
    protected abstract fun matches(firing: Firing): Collection<Int>

    final override fun select(
        firing: Firing,
        selection: Selection,
    ) {
        val room = firing.scope.room
        val matched =
            matches(firing).filterTo(LinkedHashSet()) { id ->
                room.furni[id]?.let { Candidates.selectable(it, wallItems, anyBox) } == true
            }
        SelectionMerge.apply(
            selection.furniIds,
            settings,
            matched,
            { Candidates.furni(room, wallItems) },
            unconfigured(firing),
        )
    }
}
