package zel.wired.selector

import zel.wired.box.SelectorBox
import zel.wired.firing.Firing
import zel.wired.firing.Selection

abstract class UnitSelector : SelectorBox {
    protected open val avatarsOnly: Boolean get() = false

    protected open fun unconfigured(firing: Firing): Boolean = false

    /** Room indexes of the matched units, before the filter and invert flags. */
    protected abstract fun matches(firing: Firing): Collection<Int>

    final override fun select(
        firing: Firing,
        selection: Selection,
    ) {
        val room = firing.scope.room
        val candidates = Candidates.units(room, avatarsOnly)
        val matched = matches(firing).filterTo(LinkedHashSet()) { it in candidates }
        SelectionMerge.apply(selection.unitIndexes, settings, matched, { candidates }, unconfigured(firing))
    }
}
