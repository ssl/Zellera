package zel.room.furni.behaviour.state

import zel.room.map.Point

/** The state family's runtime state of one room; touched only under its lock. */
class RoomStates {
    val later = mutableListOf<StateJob>()

    val pyramidDue = HashMap<Int, Long>()

    val voted = HashMap<Int, MutableSet<Int>>()

    val coolUntil = HashMap<Int, Long>()

    val playingSince = HashMap<Int, Long>()

    /** The room's first tick: a machine already on when the room loads plays from here. */
    var firstTickAt: Long? = null

    val playlists = HashMap<Int, List<Int>>()

    /** The tiles each area hide that is on hides; filled from the stored states on first use (AreaHide.held). */
    val hidden = HashMap<Int, List<Point>>()

    var hiddenLoaded = false

    /**
     * The wall area each area hide with "hide wall items" ticked hides, from when it was switched on or changed in
     * this load; never filled from the stored states (AreaHide.refresh).
     */
    val hiddenWalls = HashMap<Int, WallArea>()

    var chooserHeld = false

    /** Whether the viewers have the invisible furni out of view; null until the first viewer is sent the room. */
    var furniHidden: Boolean? = null

    val gym = HashMap<Int, GymSession>()

    val ignited = HashSet<Int>()
}
