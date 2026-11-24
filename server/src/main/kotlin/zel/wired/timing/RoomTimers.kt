package zel.wired.timing

class RoomTimers(
    private val origin: Long,
) {
    var resetPending = false

    var shortQueued = false

    var started = false
        private set
    private var restartedAt = origin
    private val repeats = HashMap<Int, RepeatClock>()
    private val onces = HashMap<Int, OnceClock>()

    fun start() {
        started = true
    }

    fun markReset(now: Long) {
        resetPending = false
        restartedAt = now
    }

    fun elapsed(now: Long): Long = now - restartedAt

    fun keep(triggerIds: Set<Int>) {
        repeats.keys.retainAll(triggerIds)
        onces.keys.retainAll(triggerIds)
    }

    fun nextDue(
        trigger: RepeatTrigger,
        now: Long,
    ): Long = clock(trigger, now).nextDue

    fun repeatDue(
        trigger: RepeatTrigger,
        now: Long,
        slack: Long,
        reset: Boolean = false,
    ): Boolean {
        val clock = clock(trigger, now)
        if (reset) {
            clock.nextDue = now + trigger.periodMillis
            return true
        }
        if (now < clock.nextDue - slack) return false
        while (clock.nextDue <= now + slack) clock.nextDue += trigger.periodMillis
        return true
    }

    fun onceDue(
        trigger: OnceTrigger,
        now: Long,
        slack: Long,
        reset: Boolean = false,
    ): Boolean {
        val clock =
            onces[trigger.furniId]?.takeIf { it.box === trigger }
                ?: OnceClock(trigger, if (started) null else origin + trigger.afterMillis).also {
                    onces[trigger.furniId] = it
                }
        if (reset) clock.due = now + trigger.afterMillis
        val due = clock.due ?: return false
        if (now < due - slack) return false
        clock.due = null
        return true
    }

    /**
     * A repeater of the loaded room is due at the load itself, so it fires once at the start; one saved later counts
     * from the save.
     */
    private fun clock(
        trigger: RepeatTrigger,
        now: Long,
    ): RepeatClock =
        repeats[trigger.furniId]?.takeIf { it.box === trigger }
            ?: RepeatClock(trigger, if (started) now + trigger.periodMillis else origin).also {
                repeats[trigger.furniId] = it
            }

    private class RepeatClock(
        val box: RepeatTrigger,
        var nextDue: Long,
    )

    private class OnceClock(
        val box: OnceTrigger,
        var due: Long?,
    )
}
