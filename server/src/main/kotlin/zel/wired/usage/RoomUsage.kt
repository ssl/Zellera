package zel.wired.usage

import java.util.EnumMap

/** Guarded by the room's lock. */
class RoomUsage(
    val allowance: UsageAllowance,
    runLimits: () -> RunBudget.Limits = { RunBudget.Limits() },
) {
    val budget = RunBudget(runLimits)

    val meter = UsageMeter(allowance.usage)
    val errors = ErrorTable()
    private val lastNotice = EnumMap<WiredError, Long>(WiredError::class.java)

    /** True the first time and then at most once per [NOTICE_MILLIS]: the error is then counted and logged. */
    fun notice(
        error: WiredError,
        now: Long,
    ): Boolean {
        val last = lastNotice[error]
        if (last != null && now - last < NOTICE_MILLIS) return false
        lastNotice[error] = now
        return true
    }

    companion object {
        const val NOTICE_MILLIS = 20_000L
    }
}
