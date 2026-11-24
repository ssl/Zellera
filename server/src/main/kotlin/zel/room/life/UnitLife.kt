package zel.room.life

/** Owned by the room's lock. */
class UnitLife {
    var idleTicks: Int = 0

    var asleep: Boolean = false

    var handItem: Int = 0

    /**
     * `@handitem` given by wired with an empty hand: held with the value 0. Each room entry has a new life, so it
     * starts off; a hand emptied any other way takes it away again.
     */
    var handVariable: Boolean = false

    /** The avatar effect (0 = none) and when it ends (null = never). */
    var effect: Int = 0
    var effectEndsAt: Long? = null
}
