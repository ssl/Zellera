package zel.wired.box

import zel.wired.firing.Firing

/** [negative] effects run only when the conditions fail. */
interface EffectBox : WiredBox {
    val negative: Boolean
    val needsUser: Boolean

    fun run(firing: Firing)
}
