package zel.wired.condition

import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing

class DateRangeActive(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val clock: WiredClock,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val start = settings.intAt(0).coerceAtLeast(0).toLong()
        val end = settings.intAt(1).coerceAtLeast(0).toLong()
        if (start == 0L && end == 0L) return false
        val now = clock.now(firing.scope.room).toEpochSecond()
        return now >= start && (end == 0L || now <= end)
    }
}
