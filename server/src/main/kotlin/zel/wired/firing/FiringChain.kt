package zel.wired.firing

import zel.core.rules.rules

class FiringChain private constructor(
    val depth: Int,
    private val shared: Shared,
    /** The stacks a delayed call runs, and what they start: no box blinks, no antenna flip goes out. */
    val quiet: Boolean = false,
) {
    constructor() : this(0, Shared())

    fun nested(): FiringChain = FiringChain(depth + 1, shared, quiet)

    fun quieted(): FiringChain = if (quiet) this else FiringChain(depth, shared, true)

    /** A quiet chain flips each antenna once, as a firing does, without a flash to tell. */
    fun flipsOnce(antennaId: Int): Boolean = shared.flipped.add(antennaId)

    fun enter(stackId: Int): Boolean = shared.running.add(stackId)

    fun isRunning(stackId: Int): Boolean = stackId in shared.running

    fun leave(stackId: Int) {
        shared.running.remove(stackId)
    }

    /** A signal to [stackId] while it is still on the chain, held to run once the stack has left it. */
    fun hold(
        stackId: Int,
        run: () -> Unit,
    ) {
        shared.held.getOrPut(stackId) { ArrayList() } += run
    }

    /** The signals [hold] kept for [stackId], taken out. */
    fun takeHeld(stackId: Int): List<() -> Unit> = shared.held.remove(stackId).orEmpty()

    /**
     * Runs [effects] and queues the signal deliveries they return. Only the outermost call of the chain delivers: it
     * takes the queue in order until it is empty, so a receiver's own sends go after the ones already waiting
     * (breadth-first): a stack sending to two receivers runs the second before the first's own receivers. A quiet
     * chain keeps its own queue, so the stacks a delayed call runs deliver theirs while still quiet.
     */
    fun delivering(effects: () -> List<() -> Unit>) {
        val queue = if (quiet) shared.quietSends else shared.sends
        if (queue.delivering) {
            queue.waiting += effects()
            return
        }
        queue.delivering = true
        try {
            queue.waiting += effects()
            while (queue.waiting.isNotEmpty()) queue.waiting.removeFirst()()
        } finally {
            queue.delivering = false
            queue.waiting.clear()
        }
    }

    fun takeSignal(): Boolean = shared.signals++ < rules.limits.wiredSignalsPerFiring

    /**
     * Counts one run of [stackId] a variable change started in this chain; false once it ran [limit] times. [running]:
     * the stack is running now, which counts as its first run when nothing counted it yet.
     */
    fun takeVariableRun(
        stackId: Int,
        limit: Int,
        running: Boolean = false,
    ): Boolean {
        val runs = shared.variableRuns.getOrDefault(stackId, if (running) 1 else 0)
        if (runs >= limit) return false
        shared.variableRuns[stackId] = runs + 1
        return true
    }

    fun tooDeep(maxDepth: Int): Boolean = depth > maxDepth

    fun claim(stackId: Int): Boolean = shared.fired.add(stackId)

    fun hasFired(stackId: Int): Boolean = stackId in shared.fired

    private class Shared {
        val fired = HashSet<Int>()
        val running = HashSet<Int>()
        val held = HashMap<Int, MutableList<() -> Unit>>()
        var signals = 0
        val variableRuns = HashMap<Int, Int>()
        val flipped = HashSet<Int>()
        val sends = Sends()
        val quietSends = Sends()
    }

    private class Sends {
        val waiting = ArrayDeque<() -> Unit>()
        var delivering = false
    }
}
