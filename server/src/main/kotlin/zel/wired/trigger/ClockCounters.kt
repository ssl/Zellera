package zel.wired.trigger

import zel.room.RoomScope
import zel.wired.WiredRooms
import zel.wired.firing.FiringStart

class ClockCounters(
    private val wired: WiredRooms,
) {
    /** Called under the room's lock, on the thread that counts. */
    fun counted(
        scope: RoomScope,
        clockId: Int,
        millis: Int,
    ) {
        if (WiredRooms.isOff(scope.room)) return
        wired.runEach(scope, null) { stack ->
            FiringStart(null, clockId).takeIf { (stack.trigger as? ClockCounter)?.reaches(clockId, millis) == true }
        }
    }
}
