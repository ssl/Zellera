package zel.room.furni.behaviour.special

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import java.util.concurrent.atomic.AtomicBoolean

/** The special family's runtime state of one room; touched only under its lock. */
class SpecialRoom {
    val later = mutableListOf<SpecialJob>()

    /** Shown once the room's clock reaches [RoomJob.due], whatever furni still stand. */
    val due = mutableListOf<RoomJob>()

    /** By box: the one player waiting for the box's other half. */
    val mysteryWaits = LinkedHashMap<Int, MysteryWait>()

    val cracked = HashSet<Int>()

    var presets: Dimmers.Presets? = null

    val healed = HashMap<Int, Long>()

    val naming = HashMap<Int, Naming>()

    val becoming = LinkedHashSet<Becoming<*>>()

    /** Touched by both the persistence executor and the room's worker. */
    class Becoming<T : Any>(
        private val orphaned: (T) -> Unit,
    ) {
        private val settled = AtomicBoolean(false)

        @Volatile
        private var result: T? = null

        @Volatile
        private var finished = false

        @Volatile
        private var gone = false

        /** The persistence executor has finished; [made] is null when nothing came of it. */
        fun made(made: T?) {
            result = made
            finished = true
            if (gone) settle()
        }

        fun roomGone() {
            gone = true
            if (finished) settle()
        }

        fun showOnce(show: (T?) -> Unit) {
            if (settled.compareAndSet(false, true)) show(result)
        }

        private fun settle() {
            if (settled.compareAndSet(false, true)) result?.let(orphaned)
        }
    }

    data class Naming(
        val furniId: Int,
        val since: Long,
    )

    class SpecialJob(
        val due: Long,
        val furniId: Int,
        val run: (RoomScope, RoomFurni) -> Unit,
    )

    class RoomJob(
        val due: Long,
        val run: (RoomScope) -> Unit,
    )

    /** [keyId] is the key already turned, 0 while the box's owner is the one waiting. */
    data class MysteryWait(
        val boxId: Int,
        val playerId: Int,
        val keyId: Int,
    )
}
