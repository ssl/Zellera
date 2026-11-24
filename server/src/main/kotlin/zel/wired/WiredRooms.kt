package zel.wired

import org.slf4j.LoggerFactory
import zel.core.rules.rules
import zel.room.Room
import zel.room.RoomListener
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.RoomState
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.room.tick.TickPart
import zel.util.NANOS_PER_MILLI
import zel.wired.box.SelectorBox
import zel.wired.firing.FiringChain
import zel.wired.firing.FiringStart
import zel.wired.firing.FlashBatch
import zel.wired.firing.FlashOut
import zel.wired.usage.RunBudget
import zel.wired.usage.WiredError
import java.util.concurrent.ConcurrentHashMap

class WiredRooms(
    private val builder: StackBuilder,
    private val stackRun: StackRun,
    timers: TickPart = TickPart.None,
    private val maxDepth: () -> Int = { rules.limits.wiredNesting },
    /** Longest one outermost run may go on before nothing more starts inside it. */
    private val runCeilingMillis: () -> Int = { RunBudget.Limits().burstMillis },
    private val nanos: () -> Long = System::nanoTime,
    /** Whether the room's viewers have a furni; a flash leaves out the ones they do not. */
    private val inView: (Room) -> (RoomFurni) -> Boolean = { { true } },
    /**
     * A box blinks at most once in each window of this length (0: every firing); see [FlashOut.BLINK_WINDOW_MILLIS].
     */
    private val blinkWindowMillis: Long = 0,
) : RoomPart,
    RoomListener {
    private val rooms = ConcurrentHashMap<Room, RoomWired>()

    /** A batch a failed tick left open goes out first. */
    val openFlash: TickPart =
        TickPart { scope ->
            val state = state(scope.room)
            state.flash?.let { flash(scope, it) }
            state.flash = FlashBatch { state.afterLine }
        }

    /** Goes out after the tick's RoomUnitUpdate and what the steps set off. */
    val closeFlash: TickPart =
        TickPart { scope ->
            val state = state(scope.room)
            val batch = state.flash
            state.flash = null
            batch?.let { flash(scope, it) }
        }

    /** Runs due half-second timers before the units step. */
    val tick: TickPart = timers

    /**
     * A score change a says firing makes runs its stacks once the speaker's line is out, in a flash of its own: the
     * says stack's blinks, the line, then the score achieved stack's blinks and its whisper. Other events a says
     * firing raises keep running inside it.
     */
    override fun on(
        scope: RoomScope,
        event: RoomEvent,
    ) {
        if (isOff(scope.room)) return
        val state = state(scope.room)
        if (event is RoomEvent.ScoreChanged) {
            state.afterLine?.let { held ->
                held += { later -> on(later, event) }
                return
            }
        }
        val line = (event as? RoomEvent.Says)?.hiding?.afterLine
        val outer = state.afterLine
        if (line != null) state.afterLine = line
        try {
            runStacks(scope, event, FiringStart.of(event), RunMode.Event) { it }
        } finally {
            state.afterLine = outer
        }
    }

    override fun unload(scope: RoomScope) {
        rooms.remove(scope.room)
    }

    fun stacks(scope: RoomScope): List<WiredStack> = builder.build(scope.room, state(scope.room))

    fun selectors(scope: RoomScope): List<SelectorBox> {
        stacks(scope)
        return state(scope.room).placed.values.mapNotNull { it.box as? SelectorBox }
    }

    fun runStacks(
        scope: RoomScope,
        event: RoomEvent?,
        start: FiringStart,
        mode: RunMode,
        quiet: Boolean = false,
        choose: (List<WiredStack>) -> List<WiredStack>,
    ) {
        if (isHeldBack(scope)) return
        within(scope, quiet = quiet) { flash, chain ->
            val input = StackRun.Input(scope, event, start, chain, flash, mode)
            runAll(choose(stacks(scope)).map { it to input }, onePerTile = mode == RunMode.Event)
        }
    }

    /** [nest] counts the run as one level deeper, for events a running stack can raise without end. */
    fun runEach(
        scope: RoomScope,
        event: RoomEvent?,
        nest: Boolean = false,
        starts: (WiredStack) -> FiringStart?,
    ) {
        if (isHeldBack(scope)) return
        within(scope, nest) { flash, chain ->
            val runs =
                stacks(scope).mapNotNull { stack ->
                    starts(stack)?.let { stack to StackRun.Input(scope, event, it, chain, flash, RunMode.Direct) }
                }
            runAll(runs)
        }
    }

    /** [quiet] starts a run that blinks no box, for the stacks a delayed call runs (see [FiringChain.quiet]). */
    fun within(
        scope: RoomScope,
        nest: Boolean = true,
        quiet: Boolean = false,
        block: (FlashBatch, FiringChain) -> Unit,
    ) {
        val state = state(scope.room)
        val parent = state.chain
        val joined = parent?.let { if (nest) it.nested() else it } ?: FiringChain()
        val chain = if (quiet) joined.quieted() else joined
        if (chain.tooDeep(maxDepth()) || overtime(state)) {
            stackRun.overflow(scope, WiredError.RECURSION_TIMEOUT)
            return
        }
        val own = state.flash == null
        val flash = state.flash ?: FlashBatch { state.afterLine }.also { state.flash = it }
        val outermost = state.runStarted == null
        val began = nanos()
        if (outermost) state.runStarted = began
        state.chain = chain
        try {
            block(flash, chain)
        } finally {
            state.chain = parent
            if (outermost) {
                state.runStarted = null
                stackRun.spent(scope, nanos() - began)
            }
            if (own) {
                state.flash = null
                flash(scope, flash)
            }
        }
    }

    fun resume(
        scope: RoomScope,
        chain: FiringChain,
        block: () -> Unit,
    ) {
        val state = state(scope.room)
        val before = state.chain
        state.chain = chain
        try {
            within(scope) { _, _ -> block() }
        } finally {
            state.chain = before
        }
    }

    private fun flash(
        scope: RoomScope,
        batch: FlashBatch,
    ) {
        if (blinkWindowMillis > 0) {
            val window = scope.now() / blinkWindowMillis
            val blinked = state(scope.room).blinkWindows
            batch.dropBoxes { id -> blinked.put(id, window) == window }
        }
        FlashOut.send(scope, batch, state(scope.room).flashOrder::rank) { inView(scope.room) }
    }

    /** What a run under way set off is never held back, only what starts a run of its own. */
    fun isHeldBack(scope: RoomScope): Boolean = state(scope.room).runStarted == null && !stackRun.open(scope)

    /** Counts [error] in the room's error table, as the engine's own overflows are. */
    fun raise(
        scope: RoomScope,
        error: WiredError,
    ) = stackRun.overflow(scope, error)

    private fun overtime(state: RoomWired): Boolean =
        state.runStarted?.let { nanos() - it > runCeilingMillis() * NANOS_PER_MILLI } == true

    /**
     * Every stack the run fires checks its conditions before any of them runs its effects. With [onePerTile] (an
     * event) a tile fires once: of its triggers that match, only the one with the highest furni id does.
     */
    private fun runAll(
        runs: List<Pair<WiredStack, StackRun.Input>>,
        onePerTile: Boolean = false,
    ) {
        val fired = HashSet<Point>()
        val order = if (onePerTile) runs.sortedByDescending { it.first.id } else runs
        val prepared =
            order.mapNotNull { (stack, input) ->
                if (onePerTile && stack.tile in fired) return@mapNotNull null
                guarded(input.scope, stack) { stackRun.prepare(stack, input) }?.also { fired += stack.tile }
            }
        val position = runs.withIndex().associate { (index, run) -> run.first to index }
        prepared
            .sortedBy { position.getValue(it.stack) }
            .forEach { guarded(it.scope, it.stack) { stackRun.execute(it) } }
    }

    @Suppress("TooGenericExceptionCaught") // one failing stack must not keep the event from the others
    private fun <T> guarded(
        scope: RoomScope,
        stack: WiredStack,
        step: () -> T,
    ): T? =
        try {
            step()
        } catch (e: Exception) {
            log.warn("room {}: wired stack {} failed", scope.room.info.id, stack.id, e)
            null
        }

    /** A room on its way out gets a passing state, so a late caller leaves nothing behind after the unload. */
    private fun state(room: Room): RoomWired =
        if (isGoing(room)) rooms[room] ?: RoomWired() else rooms.computeIfAbsent(room) { RoomWired() }

    companion object {
        const val DISABLE_CONTROL = "control.wired"

        private val log = LoggerFactory.getLogger(WiredRooms::class.java)

        fun isGoing(room: Room): Boolean = room.state == RoomState.Unloading || room.state == RoomState.Unloaded

        fun isOff(room: Room): Boolean = room.furni.withInteraction(DISABLE_CONTROL).any { it.state == "1" }
    }
}
