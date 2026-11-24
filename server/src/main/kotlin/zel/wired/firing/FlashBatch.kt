package zel.wired.firing

import zel.room.RoomScope
import zel.room.furni.HeightMapChange
import zel.room.furni.behaviour.state.ChangeFlash

/**
 * [line]: while a says firing whose line holds runs, what goes once that line is out ([zel.wired.RoomWired.afterLine]).
 */
class FlashBatch(
    private val line: () -> MutableList<(RoomScope) -> Unit>? = { null },
) : ChangeFlash {
    private val changed = LinkedHashSet<Int>()
    private val boxes = LinkedHashSet<Int>()
    private val filterBoxes = HashSet<Int>()
    private val writes = LinkedHashMap<Int, MutableList<BoxWrite>>()
    private val after = ArrayList<(RoomScope) -> Unit>()
    private val afterSignals = ArrayList<(RoomScope) -> Unit>()
    private val first = LinkedHashMap<Int, (RoomScope) -> Unit>()

    /** The innermost signal's list takes what [later] adds; with none open, [after] does. */
    private val sinks = ArrayDeque<MutableList<(RoomScope) -> Unit>>()

    val movements = MovementBatch()

    private val updated = LinkedHashSet<Int>()
    private var heights: HeightMapChange? = null

    /**
     * A furni a wired move or lift put somewhere without a slide: one ObjectUpdate with its final place when the flash
     * goes out, however often the firing moved it: a move then an altitude change of the same furni is one
     * ObjectUpdate with the final z, then one HeightMapUpdate.
     */
    fun objectUpdate(furniId: Int) {
        updated += furniId
    }

    /** The tiles a wired move or lift changed; the flash sends each tile once, with its final height. */
    fun heights(change: HeightMapChange) {
        heights = heights?.plus(change) ?: change
    }

    /** What [objectUpdate] kept, in the order first given, taken out. */
    fun takeObjectUpdates(): List<Int> = updated.toList().also { updated.clear() }

    /** What [heights] kept, taken out. */
    fun takeHeights(): HeightMapChange? = heights.also { heights = null }

    fun changed(furniId: Int) {
        changed += furniId
    }

    fun blink(boxId: Int) {
        boxes += boxId
    }

    fun blinkFilter(boxId: Int) {
        boxes += boxId
        filterBoxes += boxId
    }

    fun inFilterMode(boxId: Int): Boolean = boxId in filterBoxes

    /** Takes the boxes [drop] says out of this flash; a furni change of the same id still goes out. */
    fun dropBoxes(drop: (Int) -> Boolean) {
        val dropped = boxes.filter { it !in changed && drop(it) }
        boxes.removeAll(dropped.toSet())
        filterBoxes.removeAll(dropped.toSet())
    }

    /** A variable box whose variable was written ([zel.wired.variable.VariableBlink] kind, values before and after). */
    fun written(
        boxId: Int,
        write: BoxWrite,
    ) {
        writes.getOrPut(boxId) { ArrayList() } += write
    }

    /** The written variable boxes, each with its writes in order. */
    fun writes(): Map<Int, List<BoxWrite>> = writes

    override fun later(send: (RoomScope) -> Unit) {
        (sinks.lastOrNull() ?: after).add(send)
    }

    /**
     * A wired message: inside a says firing whose line holds (also a stack its variable changes run), it goes once the
     * speaker's line is out, so the speaker gets the hidden line before it; a signal's list still takes it first.
     */
    fun afterLine(send: (RoomScope) -> Unit) {
        (sinks.lastOrNull() ?: line() ?: after).add(send)
    }

    @Suppress("IgnoredReturnValue") // removed first, so the key moves to the end of the order
    override fun once(
        key: Int,
        send: (RoomScope) -> Unit,
    ) {
        first.remove(key)
        first[key] = send
    }

    /** What [once] keeps, taken out: it goes out a single time. */
    fun takeFirst(): List<(RoomScope) -> Unit> = first.values.toList().also { first.clear() }

    fun signalling(
        into: MutableList<(RoomScope) -> Unit>?,
        block: () -> Unit,
    ) {
        sinks.addLast(into ?: afterSignals)
        try {
            block()
        } finally {
            sinks.removeLast()
        }
    }

    fun isEmpty(): Boolean =
        changed.isEmpty() &&
            boxes.isEmpty() &&
            writes.isEmpty() &&
            movements.isEmpty() &&
            updated.isEmpty() &&
            heights == null &&
            after.isEmpty() &&
            afterSignals.isEmpty() &&
            first.isEmpty()

    fun changedIds(): List<Int> = changed.toList()

    /** A box that is also a changed furni goes in the first group only. */
    fun boxIds(): List<Int> = boxes.filterNot { it in changed }

    /**
     * The changed furni, then the boxes with the written variable boxes among them, each group by [rank] (equal ranks
     * keep the order they came in): a variable box goes among the blinking boxes, never before a changed furni;
     * [FlashOrder] has the rank.
     */
    fun ids(rank: (Int) -> Int = { 0 }): List<Int> {
        val first = changedIds()
        val boxes = boxIds()
        val second = boxes + writes.keys.filterNot { it in changed || it in boxes }
        return first.sortedBy(rank) + second.sortedBy(rank)
    }

    fun afterFlash(): List<(RoomScope) -> Unit> = after + afterSignals
}

class BoxWrite(
    val kind: Int,
    val old: Long? = null,
    val new: Long? = null,
)
