package zel.room.tick

import zel.room.Room
import zel.room.RoomScope
import zel.room.RoomState
import java.util.concurrent.Future

class RoomTicker(
    private val room: Room,
    private val output: TickOutput,
    private val tick: RoomTick,
    private val scope: RoomScope,
    private val clock: RoomClock,
    private val report: TickReport,
) {
    /** Owned by the room lock. */
    private var future: Future<*>? = null

    private var stopped = false

    private var lastSlowReport: Long? = null

    fun start(workers: RoomWorkers) =
        room.locked {
            check(future == null) { "room ${room.info.id}: tick started twice" }
            if (!stopped) future = workers.scheduleTick(room.info.id, ::runOnce)
        }

    /**
     * Never throws, not even an [Error]: the executor cancels every later run of a tick that does, and the room
     * would stay loaded without ever ticking again.
     */
    @Suppress("TooGenericExceptionCaught") // a throwing tick would cancel every later run
    fun runOnce() {
        try {
            val took = room.locked { if (stopped || room.state != RoomState.Loaded) null else timedTick() } ?: return
            if (took > RoomTiming.SLOW_TICK_MILLIS) reportSlow(took)
        } catch (e: Throwable) {
            report.failed(room.info.id, e)
        }
    }

    fun stop() =
        room.locked {
            stopped = true
            future?.cancel(false)
        }

    @Suppress("TooGenericExceptionCaught") // a failing tick is reported and the output batch still closes
    private fun timedTick(): Long {
        val started = clock.now()
        val event = RoomTickEvent()
        event.begin()
        output.beginTick()
        try {
            tick.run(scope, output)
        } catch (e: Throwable) {
            report.failed(room.info.id, e)
        } finally {
            output.endTick()
        }
        event.end()
        if (event.shouldCommit()) {
            event.roomId = room.info.id
            event.units = room.units.all().size
            event.commit()
        }
        return clock.now() - started
    }

    private fun reportSlow(took: Long) {
        val now = clock.now()
        val last = lastSlowReport
        if (last != null && now - last < RoomTiming.SLOW_TICK_LOG_MILLIS) return
        lastSlowReport = now
        report.slow(room.info.id, took)
    }
}
