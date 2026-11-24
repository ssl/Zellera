package zel.wired.condition

import zel.room.RoomScope
import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing

class TimeCompare(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val elapsed: (RoomScope) -> Long,
    private val comparison: Int,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val limit = settings.intAt(0, FRESH).coerceIn(0, MAX_HALF_SECONDS) * HALF_SECOND_MILLIS
        return Comparison.holds(comparison, elapsed(firing.scope), limit)
    }

    companion object {
        private const val FRESH = 20
        const val MAX_HALF_SECONDS = 172_800
        private const val HALF_SECOND_MILLIS = 500L
    }
}
