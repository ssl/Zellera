package zel.wired

import zel.room.RoomScope
import zel.wired.firing.FiringChain
import zel.wired.firing.FlashBatch
import zel.wired.firing.FlashOrder

/** Read and written only under the room's lock. */
class RoomWired {
    val placed = HashMap<Int, PlacedBox>()

    var stacks: List<WiredStack> = emptyList()

    var flash: FlashBatch? = null

    /** The order inside this load's flashes ([FlashOrder]): a new load draws a new one. */
    val flashOrder = FlashOrder()

    var chain: FiringChain? = null

    /** The blink window in which each box last blinked ([WiredRooms] blink window). */
    val blinkWindows = HashMap<Int, Long>()

    /** While a says event runs: what goes once the speaker's line is out ([WiredRooms.on]). */
    @Suppress("DoubleMutabilityForCollection") // points at the list of the line that is speaking, swapped per run
    var afterLine: MutableList<(RoomScope) -> Unit>? = null

    /** When the outermost run under way began, in nanoseconds. */
    var runStarted: Long? = null
}
