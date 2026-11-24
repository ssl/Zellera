package zel.wired.condition

import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing

class MatchTime(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val clock: WiredClock,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val now = clock.now(firing.scope.room)
        return part(0, now.second) && part(1, now.minute) && part(2, now.hour)
    }

    private fun part(
        part: Int,
        value: Int,
    ): Boolean {
        val used = settings.intAt(part) != 0
        val first = USED + part * 2
        return !used || ClockRange.contains(value, settings.intAt(first), settings.intAt(first + 1))
    }

    private companion object {
        const val USED = 3
    }
}
