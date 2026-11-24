package zel.room

import zel.room.furni.FurniSight
import zel.room.furni.RoomFurniSet
import zel.room.map.FloorPlan
import zel.room.map.Point
import zel.room.unit.RoomUnits
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Room(
    info: RoomInfo,
    floor: FloorPlan,
) {
    private val lock = ReentrantLock()

    @Volatile
    var info: RoomInfo = info
        private set

    var floor: FloorPlan = floor
        private set

    /** Where the room is in its life; only a [RoomState.Loaded] room runs game logic. */
    var state: RoomState = RoomState.Loading
        private set

    val furni = RoomFurniSet()
    val units = RoomUnits()

    val sight = FurniSight(this)

    /** A game extension wants to hear clicks on users; owned by the room lock. */
    var hearsUserClicks = false

    /** Players watching without an avatar; they hear what the room hears. */
    val spectators = LinkedHashSet<Int>()

    /** Closed to walks by a game extension; owned by the room lock. */
    val closedTiles = HashSet<Point>()

    val isLockedHere: Boolean get() = lock.isHeldByCurrentThread

    fun <T> locked(block: (Room) -> T): T = lock.withLock { block(this) }

    fun updateInfo(newInfo: RoomInfo) {
        checkLocked("settings change")
        info = newInfo
    }

    fun moveTo(next: RoomState) {
        checkLocked("state change")
        check(RoomState.order.indexOf(next) > RoomState.order.indexOf(state)) {
            "room ${info.id}: $state cannot go to $next"
        }
        state = next
    }

    private fun checkLocked(what: String) = check(isLockedHere) { "room ${info.id}: $what outside the room lock" }
}
