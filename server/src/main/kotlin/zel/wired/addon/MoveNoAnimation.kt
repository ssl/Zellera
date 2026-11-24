package zel.wired.addon

import zel.wired.box.BoxSettings
import zel.wired.effect.furni.MoveAddon
import zel.wired.effect.furni.MoveStyle
import zel.wired.firing.Firing

class MoveNoAnimation(
    override val furniId: Int,
    override val settings: BoxSettings,
) : MoveAddon {
    override fun style(
        firing: Firing,
        style: MoveStyle,
    ): MoveStyle = style.copy(animated = false)
}
