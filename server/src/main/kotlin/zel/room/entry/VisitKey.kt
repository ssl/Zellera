package zel.room.entry

/** One write lane per visit, so a visit's entry log row is written before its exit closes it. */
internal data class VisitKey(
    val roomId: Int,
    val playerId: Int,
)
