package zel.room.furni

import zel.protocol.generated.ObjectAddOut
import zel.protocol.generated.ObjectAddOutCodec
import zel.protocol.generated.ObjectRemoveMultipleOut
import zel.protocol.generated.ObjectRemoveMultipleOutCodec
import zel.protocol.generated.ObjectRemoveOut
import zel.protocol.generated.ObjectRemoveOutCodec
import zel.protocol.generated.ObjectsOut
import zel.protocol.generated.ObjectsOutCodec
import zel.protocol.generated.OutgoingCodec
import zel.room.Room
import zel.room.map.Point
import kotlin.math.abs

/** A room with a view distance: each player has only the floor furni near their avatar. */
class FurniSight(
    private val room: Room,
) {
    /** Tiles around the avatar, measured along the screen's axes; 0 shows the whole room. */
    var distance: Int = 0

    private val seen = HashMap<Int, IntSet>()

    /** Players with a distance of their own (a larger screen, a view zoomed out). */
    private val own = HashMap<Int, Int>()

    val active: Boolean get() = distance > 0

    fun forget(playerId: Int) {
        seen.remove(playerId)
        own.remove(playerId)
    }

    /** [tiles] for this player from now on, until they leave. */
    fun reach(
        playerId: Int,
        tiles: Int,
    ) {
        own[playerId] = tiles
    }

    private fun distanceOf(playerId: Int): Int = own[playerId] ?: distance

    /** The furni messages each of [players] gets; null when [message] is no floor furni add or removal. */
    @Suppress("UNCHECKED_CAST") // the codec tells the message's type
    fun <T> split(
        players: List<Int>,
        codec: OutgoingCodec<T>,
        message: T,
    ): List<Pair<Int, T>>? =
        when (codec) {
            ObjectsOutCodec -> {
                players.mapNotNull { p -> objects(p, message as ObjectsOut, players.size == 1)?.let { p to it as T } }
            }

            ObjectAddOutCodec -> {
                players.filter { added(it, (message as ObjectAddOut).int1) }.map { it to message }
            }

            ObjectRemoveOutCodec -> {
                val id = (message as ObjectRemoveOut).string1.toIntOrNull()
                players.filter { removed(it, id) }.map { it to message }
            }

            ObjectRemoveMultipleOutCodec -> {
                players.mapNotNull { p -> removals(p, message as ObjectRemoveMultipleOut)?.let { p to it as T } }
            }

            else -> {
                null
            }
        }

    /** What came into [playerId]'s view and what left it since the last call. */
    fun changes(playerId: Int): Changes {
        val have = seen.getOrPut(playerId) { IntSet() }
        val center = centerOf(playerId)
        val reach = distanceOf(playerId)
        val enters = ArrayList<RoomFurni>()
        val leaves = ArrayList<Int>()
        for (furni in room.furni.all()) {
            val anchor = furni.anchor ?: continue
            if (furni.isWall) continue
            if (furni.id in have) {
                if (!near(center, anchor, reach + SLACK)) leaves += furni.id
            } else if (near(center, anchor, reach)) {
                enters += furni
            }
        }
        return Changes(enters, leaves)
    }

    fun players(): Set<Int> = seen.keys

    private fun objects(
        playerId: Int,
        message: ObjectsOut,
        keepEmpty: Boolean,
    ): ObjectsOut? {
        val have = seen.getOrPut(playerId) { IntSet() }
        val center = centerOf(playerId)
        val reach = distanceOf(playerId)
        val kept = message.list2.filter { inView(center, it.int1, reach) }
        if (kept.isEmpty() && message.list2.isNotEmpty() && !keepEmpty) return null
        kept.forEach { have.add(it.int1) }
        return if (kept.size == message.list2.size) message else message.copy(list2 = kept)
    }

    private fun added(
        playerId: Int,
        furniId: Int,
    ): Boolean {
        if (!inView(centerOf(playerId), furniId, distanceOf(playerId))) return false
        seen.getOrPut(playerId) { IntSet() }.add(furniId)
        return true
    }

    private fun removed(
        playerId: Int,
        furniId: Int?,
    ): Boolean = furniId == null || seen[playerId]?.remove(furniId) == true

    private fun removals(
        playerId: Int,
        message: ObjectRemoveMultipleOut,
    ): ObjectRemoveMultipleOut? {
        val have = seen[playerId] ?: return null
        val gone = message.list1.filter { have.remove(it) }
        return if (gone.isEmpty()) null else message.copy(list1 = gone)
    }

    private fun inView(
        center: Point,
        furniId: Int,
        reach: Int,
    ): Boolean {
        val furni = room.furni[furniId] ?: return true
        val anchor = furni.anchor ?: return true
        return furni.isWall || near(center, anchor, reach)
    }

    private fun centerOf(playerId: Int): Point = room.units.ofPlayer(playerId)?.tile ?: room.floor.door.tile

    private fun near(
        center: Point,
        tile: Point,
        reach: Int,
    ): Boolean {
        val dx = tile.x - center.x
        val dy = tile.y - center.y
        return abs(dx - dy) <= reach && abs(dx + dy) <= 2 * reach
    }

    class Changes(
        val enters: List<RoomFurni>,
        val leaves: List<Int>,
    )

    /** Furni ids, without a boxed Integer per lookup. */
    private class IntSet {
        private var slots = IntArray(INITIAL_SLOTS)
        private var size = 0

        operator fun contains(id: Int): Boolean = slots[find(id)] == id

        fun add(id: Int) {
            val at = find(id)
            if (slots[at] == id) return
            slots[at] = id
            if (++size * 2 > slots.size) grow()
        }

        fun remove(id: Int): Boolean {
            var at = find(id)
            if (slots[at] != id) return false
            size--
            // close the gap so later ids of the same run stay reachable
            var next = (at + 1) and (slots.size - 1)
            while (slots[next] != EMPTY) {
                val home = spread(slots[next]) and (slots.size - 1)
                val stays = if (at <= next) home in (at + 1)..next else home > at || home <= next
                if (!stays) {
                    slots[at] = slots[next]
                    at = next
                }
                next = (next + 1) and (slots.size - 1)
            }
            slots[at] = EMPTY
            return true
        }

        private fun find(id: Int): Int {
            var at = spread(id) and (slots.size - 1)
            while (slots[at] != EMPTY && slots[at] != id) at = (at + 1) and (slots.size - 1)
            return at
        }

        private fun grow() {
            val old = slots
            slots = IntArray(old.size * 2)
            for (id in old) if (id != EMPTY) slots[find(id)] = id
        }

        private fun spread(id: Int): Int = (id * MIX) ushr SHIFT

        private companion object {
            /** No furni has id 0. */
            const val EMPTY = 0
            const val INITIAL_SLOTS = 1024
            const val MIX = -0x61c88647
            const val SHIFT = 7
        }
    }

    private companion object {
        /** Furni leave a view this much later than they enter it. */
        const val SLACK = 8
    }
}
