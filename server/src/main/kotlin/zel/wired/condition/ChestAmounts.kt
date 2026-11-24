package zel.wired.condition

import zel.wired.box.WiredBox
import zel.wired.firing.Firing

fun interface ChestAmounts {
    fun amount(
        firing: Firing,
        box: WiredBox,
    ): Long?

    companion object {
        val TYPED: ChestAmounts =
            ChestAmounts { _, box ->
                if (box.settings.intAt(1) != 0) {
                    null
                } else {
                    box.settings
                        .intAt(0)
                        .coerceAtLeast(0)
                        .toLong()
                }
            }
    }
}
