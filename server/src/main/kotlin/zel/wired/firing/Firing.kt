package zel.wired.firing

import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.wired.box.AddonBox
import zel.wired.box.EffectBox
import zel.wired.box.SelectionFilter
import zel.wired.usage.StepBudget

class Firing(
    val scope: RoomScope,
    /** Null for timer firings. */
    val event: RoomEvent?,
    val start: FiringStart,
    val selection: Selection,
    val chain: FiringChain,
    val flash: FlashBatch,
    val filters: List<SelectionFilter>,
    val steps: StepBudget = StepBudget(),
    val addons: List<AddonBox> = emptyList(),
    val effects: List<EffectBox> = emptyList(),
    val triggerBox: Int? = null,
    /** A delayed run shares its origin's. */
    context: MutableMap<String, HeldValue>? = null,
    /** A delayed batch's run: the stacks its calls run blink nothing. */
    val delayed: Boolean = false,
) {
    val triggeringUnit: Int? get() = start.unit
    val triggeringFurni: Int? get() = start.furni
    val signal: SignalPayload? get() = start.signal

    val moved = LinkedHashSet<Int>()

    val joinedTeam: MutableSet<Int> = HashSet()

    var reachedCount = 0

    private val ends = LinkedHashMap<Any, FiringEnd>()

    /** The [FiringEnd] of [key] for this firing, made on first use; it settles once, when the effects are done. */
    @Suppress("UNCHECKED_CAST")
    fun <T : FiringEnd> atEnd(
        key: Any,
        make: () -> T,
    ): T = ends.getOrPut(key, make) as T

    @Suppress("DoubleMutabilityForCollection") // the list of the innermost run, swapped back after it
    private var sends: MutableList<() -> Unit>? = null

    /**
     * What a send signal effect of this firing hands to its receivers: inside [holdingSends] it waits until every
     * effect of that run has run, otherwise it goes now.
     */
    fun send(delivery: () -> Unit) {
        val held = sends
        if (held != null) held += delivery else delivery()
    }

    /**
     * Runs [effects] and then hands the signals they sent to the chain's queue (see [FiringChain.delivering]): they go
     * in send order after the ones already waiting, still inside this firing's flash.
     */
    fun holdingSends(effects: () -> Unit) =
        chain.delivering {
            val held = ArrayList<() -> Unit>()
            val outer = sends
            sends = held
            try {
                effects()
            } finally {
                sends = outer
            }
            held
        }

    /** Settles what effects of this firing left for its end, each once; called after its last effect ran. */
    fun settle() {
        while (ends.isNotEmpty()) {
            val key = ends.keys.first()
            ends.remove(key)?.settle()
        }
    }

    val context: MutableMap<String, HeldValue> =
        context
            ?: start.context
            ?: start.signal
                ?.contextValues
                ?.mapValuesTo(LinkedHashMap()) { HeldValue(it.value, 0, 0) }
            ?: LinkedHashMap()
}

/** What effects of one firing net out together once all of them ran (see [Firing.atEnd]). */
fun interface FiringEnd {
    fun settle()
}
