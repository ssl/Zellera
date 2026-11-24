package zel.room.listing

import zel.room.DoorMode

/** An invisible room is listed for its owner, for players with rights in it and for staff who enter any room. */
object RoomSight {
    private val INVISIBLE = DoorMode.codeOf(DoorMode.INVISIBLE)

    fun isInvisible(room: ListedRoom): Boolean = room.doorCode == INVISIBLE

    /** [rightsIn] names the rooms among the given ids the viewer has rights in; it is asked only when needed. */
    fun visible(
        found: List<ListedRoom>,
        viewerId: Int,
        seesAll: Boolean,
        rightsIn: (List<Int>) -> Set<Int>,
    ): List<ListedRoom> {
        if (seesAll) return found
        val closed = found.filter { isInvisible(it) && it.ownerId != viewerId }.map { it.id }
        if (closed.isEmpty()) return found
        val hidden = closed.toSet() - rightsIn(closed)
        return found.filterNot { it.id in hidden }
    }
}
