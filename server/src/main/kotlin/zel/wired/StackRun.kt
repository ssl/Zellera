package zel.wired

import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.wired.addon.ExecuteInOrder
import zel.wired.addon.PlaceholderBox
import zel.wired.box.AddonBox
import zel.wired.box.ConditionBox
import zel.wired.box.EffectBox
import zel.wired.box.EffectPicker
import zel.wired.box.FiringLimit
import zel.wired.box.NoTrigger
import zel.wired.box.SelectionFilter
import zel.wired.box.SelectorBox
import zel.wired.firing.Firing
import zel.wired.firing.FiringChain
import zel.wired.firing.FiringStart
import zel.wired.firing.FlashBatch
import zel.wired.trigger.LineCapture
import zel.wired.trigger.SaysSomething
import zel.wired.usage.WiredError
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class StackRun(
    private val gate: FiringGate,
    private val delays: EffectDelays,
) {
    class Input(
        val scope: RoomScope,
        val event: RoomEvent?,
        val start: FiringStart,
        val chain: FiringChain,
        val flash: FlashBatch,
        val mode: RunMode,
    )

    /** A stack whose trigger matched, with its selection made and its conditions already checked. */
    class Prepared internal constructor(
        val stack: WiredStack,
        internal val firing: Firing,
        internal val held: Boolean,
        /** The checked conditions whose own result passed; only these blink when the stack holds. */
        internal val passed: Set<Int>,
        internal val mode: RunMode,
    ) {
        val scope: RoomScope get() = firing.scope
    }

    fun run(
        stack: WiredStack,
        input: Input,
    ) {
        prepare(stack, input)?.let(::execute)
    }

    /**
     * Trigger match, selectors and conditions, with no effect run yet. Of several stacks one event fires, every one
     * is prepared before any is [execute]d, so one stack's effects never change what another's conditions see.
     */
    fun prepare(
        stack: WiredStack,
        input: Input,
    ): Prepared? {
        val firing = firing(stack, input)
        if (input.mode == RunMode.Event && !triggered(stack, firing)) return null
        if (input.chain.isRunning(stack.id)) return null
        select(stack, firing)
        if (!stack.trigger.answers(firing)) return null
        // A called stack checks none of its conditions: none blinks and its positive effects run, also for a negative
        // call.
        val check = if (input.mode is RunMode.Call) Check(true, emptySet()) else conditionsHold(stack, firing)
        return Prepared(stack, firing, check.held, check.passed, input.mode)
    }

    /** Limits, usage cap, blink and effects, by the conditions [prepare] checked. */
    fun execute(prepared: Prepared) {
        val stack = prepared.stack
        val chain = prepared.firing.chain
        if (!chain.enter(stack.id)) return
        try {
            fire(stack, prepared.firing, prepared.held, prepared.passed, prepared.mode)
        } finally {
            chain.leave(stack.id)
        }
        // A signal back to this stack while it ran (a re-roll loop) runs now, inside the same firing.
        chain.takeHeld(stack.id).forEach { it() }
    }

    fun overflow(
        scope: RoomScope,
        error: WiredError,
    ) = gate.overflow(scope, error)

    fun open(scope: RoomScope): Boolean = gate.open(scope)

    fun spent(
        scope: RoomScope,
        nanos: Long,
    ) = gate.spent(scope, nanos)

    @Suppress("CyclomaticComplexMethod") // one firing, step by step, in its fixed order
    private fun fire(
        stack: WiredStack,
        firing: Firing,
        held: Boolean,
        passed: Set<Int>,
        mode: RunMode,
    ) {
        val effects = stack.effects.filter { it.negative != held }
        val runs = held || effects.isNotEmpty()
        val limits = if (runs) stack.addons.filterIsInstance<FiringLimit>() else emptyList()
        // A firing past an execution limit is dropped before anything blinks, except a says firing on a tile with more
        // than one trigger, which blinks its trigger, selectors and the add-ons before the condition check.
        if (!limits.all { it.admits(firing) }) {
            if (mode == RunMode.Event && stack.trigger is SaysSomething && stack.triggersOnTile > 1) {
                firing.blinkBox(stack.trigger.furniId)
                stack.addons.filter(::beforeConditions).forEach { firing.blinkBox(it.furniId) }
                stack.selectors.forEach { blink(it, firing) }
            }
            // A refused call blinks the limit.
            if (mode is RunMode.Call) limits.forEach { firing.blinkBox(it.furniId) }
            return
        }
        if (!gate.admits(firing, stack, called = mode is RunMode.Call)) return
        limits.forEach { it.record(firing) }
        try {
            if (held && mode == RunMode.Event) stack.trigger.passed(firing)
            // A called stack's trigger did not fire and stays dark.
            if (stack.trigger !is NoTrigger && mode !is RunMode.Call) firing.blinkBox(stack.trigger.furniId)
            // A condition that failed inside a passing OR group stays dark.
            if (runs && held) stack.blinking.filter { it.furniId in passed }.forEach { firing.blinkBox(it.furniId) }
            // The failed side blinks only the boxes that work before the condition check, and the text outputs and
            // the execute-in-order when negative effects run; the passing side leaves dark an add-on that did nothing,
            // asking some only once the effects ran.
            val addons =
                (if (held) stack.addons else stack.addons.filter { failedSideBlinks(it, runs) })
                    .filter { it.furniId !in stack.looped }
            val (late, early) = addons.partition { runs && it.blinksAfterEffects }
            val blinks: (AddonBox) -> Boolean =
                if (mode is RunMode.Call) { addon -> addon.blinksInCall(firing) } else { addon -> addon.blinks(firing) }
            early.filter(blinks).forEach { firing.blinkBox(it.furniId) }
            // A called stack's selectors stay dark.
            if (mode !is RunMode.Call) stack.selectors.forEach { blink(it, firing) }
            if (runs) runEffects(stack, firing, effects)
            late.filter(blinks).forEach { firing.blinkBox(it.furniId) }
        } finally {
            gate.ran(firing)
        }
    }

    private fun beforeConditions(addon: AddonBox): Boolean =
        addon is SelectionFilter || addon is FiringLimit || addon is LineCapture

    /**
     * The add-ons a failed firing blinks: those before the condition check, and the text outputs and the
     * execute-in-order if negatives run.
     */
    private fun failedSideBlinks(
        addon: AddonBox,
        runs: Boolean,
    ): Boolean = beforeConditions(addon) || (runs && (addon is PlaceholderBox || addon is ExecuteInOrder))

    private fun blink(
        selector: SelectorBox,
        firing: Firing,
    ) {
        if (firing.chain.quiet) return
        if (selector.settings.filter && selector.showsFilterState) {
            firing.flash.blinkFilter(selector.furniId)
        } else {
            firing.blinkBox(selector.furniId)
        }
    }

    /** The stacks a delayed call runs, and the signals they send, blink no box. */
    private fun Firing.blinkBox(boxId: Int) {
        if (!chain.quiet) flash.blink(boxId)
    }

    private fun firing(
        stack: WiredStack,
        input: Input,
    ): Firing {
        val filters =
            stack.addons
                .filterIsInstance<SelectionFilter>()
                .sortedWith(compareBy<SelectionFilter> { it.rank }.thenBy { it.furniId })
        val start = if (input.mode == RunMode.Event) stack.trigger.started(input.start) else input.start
        return Firing(
            input.scope,
            input.event,
            start,
            start.selection.copy(),
            input.chain,
            input.flash,
            filters,
            addons = stack.addons,
            effects = stack.effects,
            triggerBox = stack.trigger.furniId.takeIf { stack.trigger !is NoTrigger },
        )
    }

    private fun triggered(
        stack: WiredStack,
        firing: Firing,
    ): Boolean {
        val event = firing.event ?: return false
        if (firing.chain.hasFired(stack.id) || !stack.trigger.matches(event, firing)) return false
        if (stack.trigger.needsUser && firing.triggeringUnit == null) return false
        return firing.chain.claim(stack.id)
    }

    private fun select(
        stack: WiredStack,
        firing: Firing,
    ) {
        firing.steps.spend(1 + stack.selectors.size + stack.checked.size)
        val (filtering, adding) = stack.selectors.partition { it.settings.filter }
        (adding + filtering).forEach { it.select(firing, firing.selection) }
        val selection = firing.selection
        firing.filters.forEach { filter ->
            val furni = filter.furni(firing, selection.furniIds.toList())
            val units = filter.units(firing, selection.unitIndexes.toList())
            selection.furniIds.retainAll(furni.toSet())
            selection.unitIndexes.retainAll(units.toSet())
        }
    }

    private class Check(
        val held: Boolean,
        val passed: Set<Int>,
    )

    private fun conditionsHold(
        stack: WiredStack,
        firing: Firing,
    ): Check {
        if (stack.checked.isEmpty() && stack.groups.isEmpty()) return Check(true, emptySet())
        val results = stack.checked.associate { it.furniId to it.passes(firing) }
        val groups = stack.groups
        val covered = groups.flatMapTo(HashSet()) { group -> group.conditions.flatMap(::leaves).map { it.furniId } }
        val alone = stack.conditions.filter { it.furniId !in covered }
        val result: (ConditionBox) -> Boolean = { box ->
            if (box is NestedGroup) box.passes(results) else results.getValue(box.furniId)
        }
        // A mode over no conditions at all is still decided: "all" and "none" pass, "at least one" does not.
        val held = groups.all { group -> group.mode.passes(group.conditions.map(result)) } && alone.all(result)
        val passed = HashSet<Int>()
        alone.filter(result).mapTo(passed) { it.furniId }
        groups
            .filter { it.mode.furniId !in stack.looped }
            .forEach { group -> blinkable(group.conditions, results, stack, passed) }
        return Check(held, passed)
    }

    /** Passing conditions blink only below picked OR add-ons that passed too and are on no pick loop. */
    private fun blinkable(
        conditions: List<ConditionBox>,
        results: Map<Int, Boolean>,
        stack: WiredStack,
        passed: MutableSet<Int>,
    ) {
        conditions.forEach { box ->
            if (box !is NestedGroup) {
                if (results.getValue(box.furniId)) passed += box.furniId
            } else if (box.furniId !in stack.looped && box.passes(results)) {
                passed += box.furniId
                blinkable(box.conditions, results, stack, passed)
            }
        }
    }

    private fun runEffects(
        stack: WiredStack,
        firing: Firing,
        effects: List<EffectBox>,
    ) {
        val picker = stack.addons.filterIsInstance<EffectPicker>().maxByOrNull { it.precedence }
        // Without an order add-on every effect runs in stack order (z, then furni id), as under execute in order: a
        // give_var below a change_var of the same variable gives first.
        val order = picker?.pick(effects, firing) ?: effects
        val delayed = LinkedHashMap<Int, MutableList<EffectBox>>()
        val runnable = order.filter { !it.needsUser || firing.triggeringUnit != null }
        // A send signal takes its place in the order like any effect, but its receivers run only once every immediate
        // effect has run: a send drawn before a roll still has the check read the new roll.
        firing.holdingSends {
            try {
                runnable.takeWhile { firing.steps.take() }.forEach { effect ->
                    firing.blinkBox(effect.furniId)
                    val delay = effect.settings.delay
                    if (delay > 0) delayed.getOrPut(delay) { mutableListOf() } += effect else runOne(effect, firing)
                }
            } finally {
                firing.settle()
            }
        }
        delayed.forEach { (delay, group) -> delays.schedule(firing, stack, delay, group) }
        if (firing.steps.exhausted) gate.overflow(firing.scope, WiredError.RECURSION_TIMEOUT)
    }

    @Suppress("TooGenericExceptionCaught") // one failing effect must not stop the others
    private fun runOne(
        effect: EffectBox,
        firing: Firing,
    ) {
        try {
            effect.run(firing)
        } catch (e: Exception) {
            log.warn("room {}: wired effect {} failed", firing.scope.room.info.id, effect.furniId, e)
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(StackRun::class.java)
    }
}
