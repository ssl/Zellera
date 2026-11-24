package zel.wired.timing

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.wired.EffectDelays
import zel.wired.FiringGate
import zel.wired.WiredRooms
import zel.wired.WiredStack
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.usage.WiredError
import java.util.concurrent.ConcurrentHashMap

/**
 * Counted down per half second while wired is on; one wake-up per room, at its earliest. Each half second is planned
 * from the one before, not from when the wake-up came, and a delay scheduled by a delayed run starts from that run's
 * planned time: a signal loop with delay 2 rolls once a second with a fixed phase, so wake-up latency must not add
 * up per step or per loop.
 */
class DelayQueue(
    private val rooms: () -> WiredRooms?,
    private val poster: RoomPoster,
    private val gate: FiringGate = FiringGate.Open,
    private val maxWaiting: () -> Int = { MAX_WAITING },
) : EffectDelays,
    RoomPart {
    private val waiting = ConcurrentHashMap<Room, Waiting>()

    fun pending(room: Room): Int = waiting[room]?.batches?.size ?: 0

    override fun schedule(
        firing: Firing,
        stack: WiredStack,
        delay: Int,
        effects: List<EffectBox>,
    ) {
        val scope = firing.scope
        val room = scope.room
        if (WiredRooms.isGoing(room)) return
        val queue = waiting.computeIfAbsent(room) { Waiting() }
        if (queue.batches.size >= maxWaiting()) {
            gate.overflow(scope, WiredError.DELAYED_EVENTS_CAP)
            return
        }
        queue.batches += Batch(delay, stack.id, effects, firing, (queue.runningDue ?: scope.now()) + FIRST_STEP)
        wake(room, queue, scope.now())
    }

    override fun unload(scope: RoomScope) {
        waiting.remove(scope.room)
    }

    private fun wake(
        room: Room,
        queue: Waiting,
        now: Long,
    ) {
        val next = queue.batches.minOfOrNull { it.nextAt } ?: return
        if (queue.wakeAt <= next) return
        val generation = ++queue.generation
        queue.wakeAt = next
        poster.post(room.info.id, (next - now).coerceAtLeast(0)) { scope ->
            if (scope.room === room && waiting[room] === queue && queue.generation == generation) awake(scope, queue)
        }
    }

    private fun awake(
        scope: RoomScope,
        queue: Waiting,
    ) {
        val now = scope.now()
        queue.wakeAt = Long.MAX_VALUE
        queue.batches
            .filter { it.nextAt <= now }
            .sortedBy { it.nextAt }
            .forEach { batch -> if (batch in queue.batches) advance(scope, queue, batch, now) }
        if (waiting[scope.room] === queue) wake(scope.room, queue, scope.now())
    }

    private fun advance(
        scope: RoomScope,
        queue: Waiting,
        batch: Batch,
        now: Long,
    ) {
        val off = WiredRooms.isOff(scope.room)
        when {
            !batch.judging -> {
                if (!off) batch.left--
                batch.judging = batch.left <= 0
                batch.nextAt += if (batch.judging) JUDGE_LEAD else TimerBoxes.HALF_SECOND
            }

            off -> {
                batch.judging = false
                batch.nextAt = now + FIRST_STEP
            }

            else -> {
                queue.batches -= batch
                queue.runningDue = batch.nextAt
                try {
                    run(scope, batch)
                } finally {
                    queue.runningDue = null
                }
            }
        }
    }

    private fun run(
        scope: RoomScope,
        batch: Batch,
    ) {
        val room = scope.room
        val wired = rooms() ?: return
        if (room.furni[batch.triggerId] == null || room.units.playerIds().isEmpty()) return
        val origin = batch.firing
        val unitGone = origin.triggeringUnit?.let { room.units[it] == null } ?: true
        // An effect box picked up during the delay still runs.
        val effects = batch.effects.filter { !(it.needsUser && unitGone) }
        if (effects.isEmpty() || wired.isHeldBack(scope)) return
        wired.within(scope) { flash, chain ->
            val firing =
                Firing(
                    scope,
                    origin.event,
                    origin.start,
                    origin.selection,
                    chain,
                    flash,
                    origin.filters,
                    addons = origin.addons,
                    triggerBox = origin.triggerBox,
                    context = origin.context,
                    delayed = true,
                )
            try {
                firing.holdingSends {
                    try {
                        effects.forEach { runOne(it, firing) }
                    } finally {
                        firing.settle()
                    }
                }
            } finally {
                gate.ran(firing)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught") // one failing effect must not stop the rest of its batch
    private fun runOne(
        effect: EffectBox,
        firing: Firing,
    ) {
        try {
            effect.run(firing)
        } catch (e: Exception) {
            log.warn("room {}: delayed wired effect {} failed", firing.scope.room.info.id, effect.furniId, e)
        }
    }

    private class Waiting {
        val batches = LinkedHashSet<Batch>()
        var wakeAt = Long.MAX_VALUE
        var generation = 0

        /** While a batch runs, the time it was due: what it schedules counts from there, not from its late run. */
        var runningDue: Long? = null
    }

    /** Half seconds are judged [JUDGE_LEAD] before they end, so a switch on the boundary counts to the next one. */
    private class Batch(
        var left: Int,
        val triggerId: Int,
        val effects: List<EffectBox>,
        val firing: Firing,
        var nextAt: Long,
        var judging: Boolean = false,
    )

    companion object {
        const val MAX_WAITING = 2000
        const val JUDGE_LEAD = 120L
        private const val FIRST_STEP = TimerBoxes.HALF_SECOND - JUDGE_LEAD
        val log: Logger = LoggerFactory.getLogger(DelayQueue::class.java)
    }
}
