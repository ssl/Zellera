package zel.wired.timing

import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.tick.RoomTiming
import zel.room.tick.TickPart
import zel.wired.RunMode
import zel.wired.WiredRooms
import zel.wired.WiredStack
import zel.wired.firing.FiringStart
import java.util.concurrent.ConcurrentHashMap

class WiredTimers(
    private val rooms: () -> WiredRooms?,
    private val poster: RoomPoster,
) : RoomPart {
    private val states = ConcurrentHashMap<Room, RoomTimers>()

    val tick: TickPart = TickPart { scope -> onTick(scope) }

    fun reset(room: Room) {
        states[room]?.resetPending = true
    }

    fun elapsed(scope: RoomScope): Long = states[scope.room]?.elapsed(scope.now()) ?: 0L

    override fun unload(scope: RoomScope) {
        states.remove(scope.room)
    }

    private fun onTick(scope: RoomScope) {
        val wired = rooms() ?: return
        val room = scope.room
        if (WiredRooms.isGoing(room)) return
        val now = scope.now()
        val state = states.computeIfAbsent(room) { RoomTimers(now - RoomTiming.TICK_MILLIS) }
        val stacks = wired.stacks(scope)
        state.keep(stacks.map { it.id }.toSet())
        val off = WiredRooms.isOff(room)
        val reset = !off && state.resetPending
        if (reset) state.markReset(now)
        // The room's first tick fires every repeater together, the 50 ms ones too; after it the short ones run on
        // their own queue.
        val first = !state.started
        val due =
            stacks.filter { stack ->
                when (val trigger = stack.trigger) {
                    is RepeatTrigger -> {
                        (!trigger.short || reset || first) &&
                            state.repeatDue(trigger, now, if (trigger.short) SHORT_SLACK else TICK_SLACK, reset)
                    }

                    is OnceTrigger -> {
                        state.onceDue(trigger, now, TICK_SLACK, reset)
                    }

                    else -> {
                        false
                    }
                }
            }
        state.start()
        if (!off) fire(scope, wired, due)
        val shorts = shortStacks(stacks)
        if (shorts.isNotEmpty() && !state.shortQueued) queueShort(room, state, shorts, now)
    }

    private fun queueShort(
        room: Room,
        state: RoomTimers,
        shorts: List<RepeatTrigger>,
        now: Long,
    ) {
        val next = shorts.minOf { state.nextDue(it, now) }
        state.shortQueued = poster.post(room.info.id, (next - now).coerceAtLeast(0)) { scope -> onShort(scope, room) }
    }

    private fun onShort(
        scope: RoomScope,
        room: Room,
    ) {
        if (scope.room !== room) return
        val state = states[room] ?: return
        state.shortQueued = false
        val wired = rooms() ?: return
        val now = scope.now()
        val stacks = wired.stacks(scope)
        val shorts = shortStacks(stacks)
        if (shorts.isEmpty()) return
        val due =
            stacks.filter { stack ->
                val trigger = stack.trigger as? RepeatTrigger
                trigger != null && trigger.short && state.repeatDue(trigger, now, SHORT_SLACK)
            }
        if (!WiredRooms.isOff(room)) fire(scope, wired, due)
        queueShort(room, state, shorts, now)
    }

    private fun fire(
        scope: RoomScope,
        wired: WiredRooms,
        due: List<WiredStack>,
    ) {
        if (due.isEmpty()) return
        val ids = due.map { it.id }.toSet()
        wired.runStacks(scope, null, FiringStart(null, null), RunMode.Direct) { stacks ->
            stacks.filter { it.id in ids }
        }
    }

    private fun shortStacks(stacks: List<WiredStack>): List<RepeatTrigger> =
        stacks.mapNotNull { (it.trigger as? RepeatTrigger)?.takeIf(RepeatTrigger::short) }

    private companion object {
        /** A tick's clock jitter: a timer due within half a tick fires in this tick. */
        const val TICK_SLACK = RoomTiming.TICK_MILLIS / 2

        const val SHORT_SLACK = RoomTiming.SHORT_TIMER_MILLIS / 2
    }
}
