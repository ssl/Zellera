package zel.room.tick

data class TickParts(
    val openFlash: TickPart = TickPart.None,
    val deferredJobs: TickPart = TickPart.None,
    val furni: TickPart = TickPart.None,
    val wiredTimers: TickPart = TickPart.None,
    val decoHosting: TickPart = TickPart.None,
    val players: TickPart = TickPart.None,
    val emptyRoom: TickPart = TickPart.None,
    val closeFlash: TickPart = TickPart.None,
    /** Runs only with players in the room. */
    val jukebox: TickPart = TickPart.None,
    val doorbell: TickPart = TickPart.None,
)
