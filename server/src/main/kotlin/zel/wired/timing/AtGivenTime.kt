package zel.wired.timing

import zel.wired.box.BoxSettings

class AtGivenTime(
    override val furniId: Int,
    override val settings: BoxSettings,
) : OnceTrigger {
    override val afterMillis: Long =
        (settings.ints.firstOrNull() ?: 1).coerceIn(1, TimerBoxes.MAX_HALF_SECONDS) * TimerBoxes.HALF_SECOND
}
