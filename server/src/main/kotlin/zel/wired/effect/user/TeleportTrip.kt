package zel.wired.effect.user

import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.room.unit.RoomUnit

/**
 * A unit's wired teleport from its first effect to its last landing; guarded by the room's lock. [landsAt] is the
 * room tick of the landing once the line on the old tile went out, null while the trip waits for [hop]'s line.
 * [next] is a teleport asked on the way: it hops from the landing, on its own line tick or the landing's, whichever
 * is later.
 */
class TeleportTrip(
    val unit: RoomUnit,
    var hop: Hop,
    val ending: TeleportEffect,
) {
    var landsAt: Long? = null

    /** The tile the line chose; the landing puts the unit there. */
    var landing: Point? = null
    var next: Hop? = null

    /** A target and the room tick of its line on the old tile. */
    class Hop(
        val target: RoomFurni,
        val lineTick: Long,
    )
}
