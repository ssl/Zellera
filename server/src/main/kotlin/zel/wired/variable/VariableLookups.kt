package zel.wired.variable

interface VariableLookups {
    fun userNames(ids: Collection<Int>): Map<Int, String>

    fun userCard(id: Int): Pair<String, String>?

    fun sharedOffers(
        ownerId: Int,
        exceptRoom: Int,
    ): List<SharedOffer>

    fun sharedBoxExists(
        roomId: Int,
        boxId: Int,
    ): Boolean

    /** The ints of a shared user or global box; null when it is gone or has no settings. */
    fun sharedBoxInts(
        roomId: Int,
        boxId: Int,
    ): List<Int>?
}
