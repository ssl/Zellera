package zel.wired.timing

import zel.wired.box.BoxSettings

class Repeater(
    override val furniId: Int,
    override val settings: BoxSettings,
    unitMillis: Long,
    maxUnits: Int,
    override val short: Boolean,
) : RepeatTrigger {
    override val periodMillis: Long = (settings.ints.firstOrNull() ?: 1).coerceIn(1, maxUnits) * unitMillis
}
