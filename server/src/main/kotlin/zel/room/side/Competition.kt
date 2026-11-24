package zel.room.side

/** One timeline: rooms enter from [entriesFrom], votes are cast from [votingFrom], and at [endsAt] it is over. */
data class Competition(
    val id: Int,
    val code: String,
    val requiredFurni: List<String>,
    val entriesFrom: Int,
    val votingFrom: Int,
    val endsAt: Int,
    val votesPerDay: Int,
    val roomsNewerThan: Int,
) {
    enum class Phase { ENTRIES, VOTING }

    fun phaseAt(at: Int): Phase? =
        when {
            at < entriesFrom || at >= endsAt -> null
            at < votingFrom -> Phase.ENTRIES
            else -> Phase.VOTING
        }

    fun takesRoomMadeAt(createdAt: Int): Boolean = roomsNewerThan <= 0 || createdAt > roomsNewerThan
}
