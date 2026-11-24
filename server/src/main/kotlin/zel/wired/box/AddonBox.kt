package zel.wired.box

import zel.wired.firing.Firing

interface AddonBox : WiredBox {
    /** Asked only after the firing's immediate effects ran: the box's part in the firing depends on what they did. */
    val blinksAfterEffects: Boolean get() = false

    /** Whether a passing firing blinks the box; one that did nothing in it stays dark. */
    fun blinks(firing: Firing): Boolean = true

    /** [blinks] for a firing of a called stack. */
    fun blinksInCall(firing: Firing): Boolean = blinks(firing)
}
