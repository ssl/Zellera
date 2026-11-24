package zel.wired.trigger

import zel.wired.box.AddonBox
import zel.wired.firing.Firing

interface LineCapture : AddonBox {
    fun capturesIn(pattern: String): Boolean

    fun capture(
        firing: Firing,
        pattern: String,
        line: String,
        contains: Boolean,
    ): Boolean
}
