package zel.wired.signal

import zel.wired.box.WiredBox
import zel.wired.firing.Firing

fun interface FrameVariables {
    fun read(
        firing: Firing,
        box: WiredBox,
        row: Int,
        target: Int,
    ): Pair<String, Long>?

    companion object {
        val None: FrameVariables = FrameVariables { _, _, _, _ -> null }
    }
}
