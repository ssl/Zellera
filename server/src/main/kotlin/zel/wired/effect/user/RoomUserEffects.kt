package zel.wired.effect.user

import zel.wired.firing.FlashBatch

/** Guarded by the room's lock. */
class RoomUserEffects {
    val frozen = HashMap<Int, Frozen>()

    /** Room ticks counted by the teleports' tick part: a teleport's line and landing fall on these ticks. */
    var ticks: Long = 0

    /** By unit index, in the order asked. */
    val teleports = LinkedHashMap<Int, TeleportTrip>()

    /** By unit index: only the latest teleport ends the effect. */
    val teleportEffects = HashMap<Int, TeleportEffect>()

    val kicking = HashSet<Int>()

    val forwards = HashMap<Int, Long>()

    val scoreUses = HashMap<Int, ScoreUses>()

    val visuals = KeptVisuals()

    /** The only furni Remove furni takes. */
    val copies = HashSet<Int>()

    val copyAllowance = CopyAllowance()

    /** The freeze netting of each flash still open (Unfreezing); dropped when the flash goes out. */
    val freezeNets = HashMap<FlashBatch, Any>()
}
