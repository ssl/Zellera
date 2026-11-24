package zel.wired.condition

import zel.room.furni.RoomFurni
import zel.room.games.ClockKind
import zel.room.games.GamesPart
import zel.util.MILLIS_PER_SECOND
import zel.util.SECONDS_PER_MINUTE
import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class CounterTimeMatches(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val games: GamesPart,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val seconds = settings.intAt(SECONDS)
        val minutes = settings.intAt(MINUTES)
        val half = if (settings.intAt(HALF_SECOND) != 0) HALF_SECOND_MILLIS else 0
        val wanted = (minutes * SECONDS_PER_MINUTE + seconds) * MILLIS_PER_SECOND + half
        val comparison = settings.intAt(COMPARISON)
        val targets = Targets.furni(firing, this)
        // No picks at all passes; picks that all left the room read as an empty set.
        if (targets.isEmpty()) return settings.picks.isEmpty() || settings.quantifier != ANY
        val counters = targets.filter { counter(it) != null }
        val checked = if (settings.quantifier == ANY) counters else targets
        return Quantified.holds(checked, settings) { furni ->
            counter(furni)?.let { Comparison.holds(comparison, it.toLong(), wanted) } == true
        }
    }

    private fun counter(furni: RoomFurni): Int? =
        games.clockOf(furni)?.takeIf { it.kind == ClockKind.GameCounter }?.millis

    companion object {
        private const val SECONDS = 0
        private const val MINUTES = 1
        private const val HALF_SECOND = 2
        private const val COMPARISON = 3
        private const val ANY = 1
        private const val HALF_SECOND_MILLIS = 500L

        fun isCounter(
            games: GamesPart,
            furni: RoomFurni,
        ): Boolean = games.clockOf(furni)?.kind == ClockKind.GameCounter
    }
}
