package zel.wired.usage

import zel.content.ClientStrings
import zel.protocol.generated.NotificationDialogOut
import zel.protocol.generated.NotificationDialogOutCodec
import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.util.NANOS_PER_MILLI
import zel.wired.FiringGate
import zel.wired.WiredStack
import zel.wired.firing.Firing
import zel.wired.store.RoomLog
import zel.wired.timing.DelayQueue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

class UsageGate(
    private val ownerLevel: (Int) -> Int,
    private val wallClock: () -> Long = System::currentTimeMillis,
    private val roomLog: (Room, RoomLog.Line) -> Unit = { _, _ -> },
    /** 0 for none; the variable counts stay the owner's level's. */
    private val fixedLimit: () -> Int = { 0 },
    private val runLimits: () -> RunBudget.Limits = { RunBudget.Limits() },
    private val delayedCap: () -> Int = { DelayQueue.MAX_WAITING },
) : FiringGate,
    RoomPart {
    private val rooms = ConcurrentHashMap<Room, RoomUsage>()

    override fun admits(
        firing: Firing,
        stack: WiredStack,
    ): Boolean = admits(firing, stack, called = false)

    override fun admits(
        firing: Firing,
        stack: WiredStack,
        called: Boolean,
    ): Boolean {
        val scope = firing.scope
        val usage = usage(scope.room)
        val now = scope.now()
        if (usage.meter.charge(now, called)) return true
        if (usage.notice(WiredError.EXECUTION_CAP, now)) capped(scope, usage)
        return false
    }

    override fun ran(firing: Firing) {
        val scope = firing.scope
        val usage = usage(scope.room)
        usage.meter.chargeAfter(scope.now(), firing.reachedCount)
    }

    override fun open(scope: RoomScope): Boolean {
        val usage = usage(scope.room)
        val now = scope.now()
        val open = usage.budget.open(now)
        raise(scope, usage)
        if (!open && !usage.budget.isPaused(now) && usage.notice(WiredError.EXECUTOR_OVERLOAD, now)) {
            told(scope, usage, WiredError.EXECUTOR_OVERLOAD)
        }
        return open
    }

    override fun spent(
        scope: RoomScope,
        nanos: Long,
    ) {
        val usage = usage(scope.room)
        usage.budget.spent(scope.now(), nanos / NANOS_PER_MILLI.toDouble())
        raise(scope, usage)
    }

    private fun raise(
        scope: RoomScope,
        usage: RoomUsage,
    ) {
        val now = scope.now()
        usage.budget.takeRaised().forEach { error ->
            if (error == WiredError.KILLED) {
                paused(scope, usage)
            } else if (usage.notice(error, now)) {
                told(scope, usage, error)
            }
        }
    }

    private fun told(
        scope: RoomScope,
        usage: RoomUsage,
        error: WiredError,
    ) {
        record(scope, usage, error)
        tellOwner(scope, if (error == WiredError.MARKED_AS_HEAVY) HEAVY_DIALOG else OVERLOAD_DIALOG)
    }

    /** Every pause is counted and told, however close to the last. */
    private fun paused(
        scope: RoomScope,
        usage: RoomUsage,
    ) {
        record(scope, usage, WiredError.KILLED)
        log.warn("room {}: wired paused after sustained overload", scope.room.info.id)
        tellOwner(scope, OVERLOAD_DIALOG)
    }

    override fun overflow(
        scope: RoomScope,
        error: WiredError,
    ) {
        val usage = usage(scope.room)
        if (!usage.notice(error, scope.now())) return
        record(scope, usage, error)
        log.warn("room {}: wired error {}", scope.room.info.id, error)
    }

    override fun unload(scope: RoomScope) {
        rooms.remove(scope.room)
    }

    /** Call under the room's lock. */
    fun usage(room: Room): RoomUsage =
        rooms.computeIfAbsent(room) {
            val allowance = UsageLimit.of(ownerLevel(it.info.ownerId))
            val fixed = fixedLimit()
            RoomUsage(if (fixed > 0) allowance.copy(usage = fixed.toDouble()) else allowance, runLimits)
        }

    private fun capped(
        scope: RoomScope,
        usage: RoomUsage,
    ) {
        record(scope, usage, WiredError.EXECUTION_CAP)
        tellOwner(scope, DIALOG)
    }

    private fun tellOwner(
        scope: RoomScope,
        dialog: NotificationDialogOut,
    ) {
        val owner = scope.room.info.ownerId
        if (owner in scope.room.units.playerIds()) {
            scope.out.send(Lane.Now, Audience.Player(owner), NotificationDialogOutCodec, dialog)
        }
    }

    private fun record(
        scope: RoomScope,
        usage: RoomUsage,
        error: WiredError,
    ) {
        val at = wallClock()
        usage.errors.record(error, at)
        val users =
            scope.room.units
                .playerIds()
                .size
        val line =
            if (error == WiredError.DELAYED_EVENTS_CAP) {
                val reading = usage.meter.reading(scope.now()).roundToInt()
                "Wired error: $error. (usage=$reading, users=$users, cap=${delayedCap()})"
            } else {
                "Wired error: $error. (usage=${usage.meter.limit.roundToInt()}, users=$users, limit=$LOG_LIMIT)"
            }
        roomLog(scope.room, RoomLog.Line(LOG_LEVEL, LOG_SOURCE, line, at))
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(UsageGate::class.java)
        const val LOG_LEVEL = 2
        const val LOG_SOURCE = 0

        /** The log line's "limit" figure. */
        const val LOG_LIMIT = 500

        val DIALOG = dialog("execution_cap")
        val OVERLOAD_DIALOG = dialog("executor_overload")
        val HEAVY_DIALOG = dialog("marked_as_heavy")

        fun dialog(text: String) =
            NotificationDialogOut(
                "wired.error",
                listOf(
                    NotificationDialogOut.List1Item("image", ClientStrings.badgeImage("WIRD2")),
                    NotificationDialogOut.List1Item("message", "\${room.error.wired.$text}"),
                ),
            )
    }
}
