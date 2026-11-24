package zel.wired.addon

import zel.wired.box.WiredBox
import zel.wired.firing.Firing

fun interface FilterAmounts {
    fun fromVariable(
        firing: Firing,
        box: WiredBox,
    ): Int

    companion object {
        val NoVariables: FilterAmounts = FilterAmounts { _, _ -> 0 }
    }
}
