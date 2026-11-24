package zel.wired.usage

import zel.core.rules.rules

class StepBudget(
    private var left: Int = rules.limits.wiredStepsPerFiring,
) {
    var exhausted = false
        private set

    fun take(): Boolean {
        if (left <= 0) {
            exhausted = true
            return false
        }
        left--
        return true
    }

    fun spend(steps: Int) {
        left -= steps
        if (left < 0) exhausted = true
    }
}
