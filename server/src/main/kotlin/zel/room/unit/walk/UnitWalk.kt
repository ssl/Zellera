package zel.room.unit.walk

import zel.protocol.generated.RoomUnitUpdateOut
import zel.room.map.Point

/** Owned by the room's lock. */
class UnitWalk {
    var steps: Int = 0

    var floorSit: Boolean = false

    /** The direction of the seat or bed the unit last turned to; null while it is not on one. */
    var seatFacing: Int? = null

    var leaving: Boolean = false

    var sentOut: Boolean = false

    var leaveAt: Long? = null

    /** The walk stopped before the door tile to leave; its door step is counted when the user leaves. */
    var doorStep: Boolean = false

    var cameByTransit: Boolean = false

    var movedByWired: Boolean = false

    /**
     * Wired moved a seat or bed onto the standing unit: it sits or lies on the room's next unit tick, after the move
     * packet, not with it.
     */
    var settleOnTick: Boolean = false

    /** The current path came from a wired box (bot_move), not from the unit's own stroll or errand. */
    var wiredWalk: Boolean = false

    var letThrough: Set<Int> = emptySet()

    var rides: Int? = null
    var riddenBy: Int? = null

    var stepOrigin: StepOrigin? = null

    var railSteps: Int = 0

    var stepLine: RoomUnitUpdateOut.List1Item? = null

    var shownTile: Point? = null
    var shownZ: Double = 0.0

    var stayLines: Int = 0

    val requests = ArrayDeque<Long>()
    var refusedGoal: Point? = null
}
