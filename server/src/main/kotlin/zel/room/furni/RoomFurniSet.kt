package zel.room.furni

import zel.room.furni.place.Interactions
import zel.room.map.Point

class RoomFurniSet {
    private val byId = LinkedHashMap<Int, RoomFurni>()
    private val changed = LinkedHashSet<RoomFurni>()
    private val byTile = HashMap<Point, MutableList<RoomFurni>>()
    private val loaded = HashSet<Int>()
    private val selections = HashMap<Any, Selection>()

    /** Goes up with every furni added or removed too. */
    var additions: Long = 0
        private set

    operator fun get(id: Int): RoomFurni? = byId[id]

    fun add(furni: RoomFurni) {
        check(byId.putIfAbsent(furni.id, furni) == null) { "furni ${furni.id} is already in the room" }
        additions++
        selections.values.forEach { it.added(furni) }
        index(furni)
        furni.standsIn = this
        if (furni.dirty) changed += furni
    }

    fun addLoaded(furni: RoomFurni) {
        add(furni)
        loaded += furni.id
    }

    fun remove(id: Int): RoomFurni? =
        byId.remove(id)?.also {
            additions++
            selections.values.forEach { selection -> selection.removed(it) }
            loaded.remove(id)
            unindex(it)
            it.standsIn = null
            changed -= it
        }

    fun move(
        furni: RoomFurni,
        anchor: Point,
        direction: Int,
    ) {
        check(byId[furni.id] === furni) { "furni ${furni.id} is not in the room" }
        unindex(furni)
        furni.relocate(anchor, direction)
        index(furni)
    }

    fun all(): Collection<RoomFurni> = byId.values

    /** They count as clean from here. */
    fun takeChanged(): List<RoomFurni> {
        if (changed.isEmpty()) return emptyList()
        val taken = changed.toList()
        changed.clear()
        taken.forEach { it.dirty = false }
        return taken
    }

    internal fun marked(
        furni: RoomFurni,
        dirty: Boolean,
    ) {
        if (dirty) changed += furni else changed -= furni
    }

    val size: Int get() = byId.size

    /** In placing order; a handed-out list never changes. */
    fun withBehaviour(type: Class<*>): List<RoomFurni> =
        selections[type]?.list ?: selecting(type) { type.isInstance(it.behaviour) }

    fun withInteraction(code: String): List<RoomFurni> =
        selections[code]?.list ?: selecting(code) { Interactions.of(it.definition) == code }

    /**
     * The furni [test] holds for, kept up to date as furni come and go. [test] must depend only on what a furni never
     * changes (its definition, its behaviour), and [key] names it.
     */
    fun selecting(
        key: Any,
        test: (RoomFurni) -> Boolean,
    ): List<RoomFurni> = selections.getOrPut(key) { Selection(test, byId.values.filter(test)) }.list

    /** Furni of the room load highest id first, then the rest in placing order. */
    fun inSweepOrder(): List<RoomFurni> {
        val (fromLoad, placed) = byId.values.partition { it.id in loaded }
        return fromLoad.sortedByDescending { it.id } + placed
    }

    fun at(tile: Point): TileStack = byTile[tile]?.let { TileStack(it.toList()) } ?: TileStack.EMPTY

    private fun index(furni: RoomFurni) {
        if (furni.isWall) return
        furni.tiles().forEach { byTile.getOrPut(it) { mutableListOf() }.add(furni) }
    }

    private fun unindex(furni: RoomFurni) {
        if (furni.isWall) return
        furni.tiles().forEach { tile ->
            byTile[tile]?.let { list ->
                list.remove(furni)
                if (list.isEmpty()) byTile.remove(tile)
            }
        }
    }

    private class Selection(
        private val test: (RoomFurni) -> Boolean,
        var list: List<RoomFurni>,
    ) {
        fun added(furni: RoomFurni) {
            if (test(furni)) list = list + furni
        }

        fun removed(furni: RoomFurni) {
            if (test(furni)) list = list.filter { it !== furni }
        }
    }
}
