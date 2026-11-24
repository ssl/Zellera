package zel.wired.addon

import zel.wired.box.AddonBox
import zel.wired.firing.Firing

interface PlaceholderBox : AddonBox {
    /** Without the `$( )` around it; empty when the box defines none. */
    val name: String

    fun text(firing: Firing): String?

    /** A called stack leaves its text outputs dark, whatever they print. */
    override fun blinksInCall(firing: Firing): Boolean = false
}
