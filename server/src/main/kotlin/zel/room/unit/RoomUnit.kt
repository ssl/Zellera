package zel.room.unit

import zel.room.map.Point

class RoomUnit(
    val index: Int,
    val kind: UnitKind,
    var tile: Point,
    var z: Double,
    var bodyDirection: Int,
    var headDirection: Int,
) {
    val statuses = LinkedHashMap<String, String>()

    var path: List<Point> = emptyList()

    var changed: Boolean = false

    /** How the unit came into the room (`@room_entry.method`, zel.room.entry.RoomEntryMethod); bots and pets 0. */
    var entryMethod: Int = 0

    /** The furni a teleporting entry arrived on (`@room_entry.teleport_id`), 0 for none. */
    var entryTeleportId: Int = 0

    val isWalking: Boolean get() = path.isNotEmpty()

    fun showsStatus(key: String): Boolean = statuses.containsKey(key)

    fun setStatus(
        key: String,
        value: String = "",
    ) {
        if (statuses.put(key, value) != value) changed = true
    }

    fun dropStatus(key: String) {
        if (statuses.remove(key) != null) changed = true
    }

    fun dropStepPostures() {
        if (statuses.keys.removeAll(UnitStatus.ENDED_BY_STEP)) changed = true
    }

    fun face(direction: Int) {
        if (bodyDirection != direction || headDirection != direction) changed = true
        bodyDirection = direction
        headDirection = direction
    }
}
