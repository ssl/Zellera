package zel.room.furni.transit

interface TransitData {
    fun partnerOf(furniId: Int): Destination?

    /** Sends [furniId] to [targetId] from now on, one way; its pair keeps its own target. */
    fun aim(
        furniId: Int,
        targetId: Int,
    )

    fun otherHopper(
        definitionId: Int,
        roomId: Int,
    ): Destination?

    fun loadTargets(roomId: Int)

    /** The furni id a teleport's room object shows it leading to, 0 for none; read once per teleport. */
    fun shownTarget(furniId: Int): Int

    /** Shows [furniId] leading to [targetId] before an [aim] queued for it has been stored. */
    fun showTarget(
        furniId: Int,
        targetId: Int,
    ) = Unit

    fun forgetTargets(furniIds: Collection<Int>)
}
