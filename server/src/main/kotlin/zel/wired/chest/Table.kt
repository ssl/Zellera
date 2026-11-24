package zel.wired.chest

/** Guarded by its room's lock. */
class Table(
    val playerId: Int,
    val deal: Deal,
    val startedAt: Long,
) {
    /** These furni are out of the inventory while they lie here. */
    val offered = LinkedHashMap<Int, OfferedFurni>()

    var acceptedAt: Long? = null

    val contractId: Int get() = (deal as? Deal.Negotiated)?.contractId ?: 0
}
