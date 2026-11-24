package zel.wired.addon

import zel.wired.box.BoxSettings
import zel.wired.effect.furni.MoveAddon
import zel.wired.effect.furni.MoveStyle
import zel.wired.firing.Firing

class MoveAnimTime(
    override val furniId: Int,
    override val settings: BoxSettings,
) : MoveAddon {
    private val millis = settings.intAt(0, FRESH_MILLIS).coerceIn(MIN_MILLIS, MAX_MILLIS)

    override fun style(
        firing: Firing,
        style: MoveStyle,
    ): MoveStyle = style.copy(duration = millis, turnsSlide = true)

    companion object {
        const val FRESH_MILLIS = 500
        private const val MIN_MILLIS = 50
        private const val MAX_MILLIS = 2000
    }
}
