package zel.wired.box

import zel.wired.firing.Firing
import zel.wired.firing.Selection

interface SelectorBox : WiredBox {
    val showsFilterState: Boolean get() = true

    fun select(
        firing: Firing,
        selection: Selection,
    )
}
