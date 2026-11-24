package zel.wired

import zel.room.RoomScope
import zel.wired.firing.Firing
import zel.wired.usage.WiredError

/** A refused firing is dropped before anything blinks. */
fun interface FiringGate {
    fun admits(
        firing: Firing,
        stack: WiredStack,
    ): Boolean

    /** [called]: the stack runs from a call, which is charged only what it reaches. */
    fun admits(
        firing: Firing,
        stack: WiredStack,
        called: Boolean,
    ): Boolean = admits(firing, stack)

    fun ran(firing: Firing) {}

    /** False while the room's wired is held back: nothing is matched, selected or run then. */
    fun open(scope: RoomScope): Boolean = true

    /** The run time of one outermost run, whatever it set off. */
    fun spent(
        scope: RoomScope,
        nanos: Long,
    ) {}

    fun overflow(
        scope: RoomScope,
        error: WiredError,
    ) {}

    companion object {
        val Open: FiringGate = FiringGate { _, _ -> true }
    }
}
