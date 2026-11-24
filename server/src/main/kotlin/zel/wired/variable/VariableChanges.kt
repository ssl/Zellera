package zel.wired.variable

import zel.wired.WiredRooms
import zel.wired.WiredStack
import zel.wired.firing.Firing
import zel.wired.firing.FiringStart
import zel.wired.firing.SignalPayload
import zel.wired.variable.box.VariableChanged

class VariableChanges(
    private val rooms: () -> WiredRooms?,
    private val listeners: List<(VariableOn, VariableChange) -> Unit> = emptyList(),
) {
    fun raise(
        on: VariableOn,
        change: VariableChange,
    ) {
        listeners.forEach { it(on, change) }
        VariableBlink.flash(on, change.boxId, VariableBlink.kindOf(change), change.oldValue, change.newValue)
        val scope = on.scope
        val wired = rooms() ?: return
        if (WiredRooms.isOff(scope.room)) return
        val unit =
            when (change.holder) {
                is Holder.Furni, Holder.Room, Holder.Context -> {
                    null
                }

                else -> {
                    scope.room.units
                        .all()
                        .firstOrNull { Holder.of(it) == change.holder }
                        ?.index
                }
            }
        val furni = (change.holder as? Holder.Furni)?.id
        val payload = SignalPayload(emptyList(), emptyList(), eventValues(change))
        val start = FiringStart(unit, furni, signal = payload)
        val firing = on.firing
        wired.runEach(scope, firing?.event, nest = true) { stack ->
            start.takeIf {
                (stack.trigger as? VariableChanged)?.watches(change) == true &&
                    !held(firing, stack, wired, start) &&
                    (firing == null || firing.chain.takeVariableRun(stack.id, LOOP_RUNS))
            }
        }
    }

    /**
     * A change to a variable a stack watches while that stack is still running (it wrote the variable itself) runs
     * it again once it is done, in the same firing, as a signal back does, so such a loop runs the stack at most
     * [LOOP_RUNS] times. The limit counts every run of the stack in the firing, reruns and runs another stack's
     * change started (per stack, not per variable), and the rerun waits until the stack is done.
     */
    private fun held(
        firing: Firing?,
        stack: WiredStack,
        wired: WiredRooms,
        start: FiringStart,
    ): Boolean {
        if (firing == null || !firing.chain.isRunning(stack.id)) return false
        if (!firing.chain.takeVariableRun(stack.id, LOOP_RUNS, running = true)) return true
        firing.chain.hold(stack.id) {
            wired.runEach(firing.scope, firing.event) { other -> start.takeIf { other.id == stack.id } }
        }
        return true
    }

    private fun eventValues(change: VariableChange): Map<String, Long> =
        buildMap {
            put(InternalIds.EVENT_BOX, change.boxId.toLong())
            put(InternalIds.EVENT_ORIGIN, change.origin.toLong())
            put(InternalIds.EVENT_TYPE, change.type.toLong())
            change.oldValue?.let { put(InternalIds.EVENT_OLD, it) }
            change.newValue?.let { put(InternalIds.EVENT_NEW, it) }
            val old = change.oldValue
            val new = change.newValue
            if (change.type == ChangeType.VALUE_CHANGED && old != null && new != null) {
                put(InternalIds.EVENT_DIFFERENCE, new - old)
            }
        }

    private companion object {
        const val LOOP_RUNS = 100
    }
}
