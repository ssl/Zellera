package zel.wired.usage

import zel.wired.box.BoxSettings
import zel.wired.box.FiringLimit
import zel.wired.firing.Firing

class ExecutionLimit(
    override val furniId: Int,
    override val settings: BoxSettings,
) : FiringLimit {
    private val max = (settings.ints.getOrNull(0) ?: MAX_FIRINGS).coerceIn(1, MAX_FIRINGS)
    private val windowMillis = (settings.ints.getOrNull(1) ?: 1).coerceIn(1, MAX_HALF_SECONDS) * HALF_SECOND

    private val admitted = ArrayDeque<Long>()

    override fun admits(firing: Firing): Boolean {
        val now = firing.scope.now()
        while (admitted.isNotEmpty() && admitted.first() <= now - windowMillis) admitted.removeFirst()
        return admitted.size < max
    }

    override fun record(firing: Firing) {
        admitted.addLast(firing.scope.now())
    }

    companion object {
        const val MAX_FIRINGS = 100
        const val MAX_HALF_SECONDS = 20
        private const val HALF_SECOND = 500L
    }
}
