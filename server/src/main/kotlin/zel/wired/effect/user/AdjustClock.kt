package zel.wired.effect.user

import zel.room.games.ClockAdjust
import zel.room.games.ClockKind
import zel.room.games.GamesPart
import zel.util.MILLIS_PER_MINUTE
import zel.util.MILLIS_PER_SECOND
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class AdjustClock(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val games: GamesPart,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val wire = settings.ints.getOrNull(OPERATOR) ?: return
        val operator = ClockAdjust.entries.firstOrNull { it.code == wire } ?: ClockAdjust.Set
        val millis = millis(settings.ints)
        Targets.furni(firing, this).forEach { furni ->
            val clock = games.clockOf(furni)?.takeIf { it.kind != ClockKind.Timer }
            clock?.let { games.clocks.adjust(firing.scope, furni, it, operator, millis) }
        }
    }

    companion object {
        const val OPERATOR = 3
        private const val SECONDS = 0
        private const val MINUTES = 1
        private const val HALF = 2
        private const val HALF_SECOND = 500

        fun millis(ints: List<Int>): Int =
            (
                (ints.getOrNull(SECONDS) ?: 0) * MILLIS_PER_SECOND +
                    (ints.getOrNull(MINUTES) ?: 0) * MILLIS_PER_MINUTE +
                    (ints.getOrNull(HALF) ?: 0) * HALF_SECOND
            ).toInt()
    }
}
