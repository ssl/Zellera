package zel.room.entry

class RoomDoor {
    val present = LinkedHashMap<Int, Present>()

    val ringing = LinkedHashMap<Int, Pending>()

    val queue = EntryQueue()

    val quitting = HashSet<Int>()

    /** Told YouArePlayingGame false before the door tile; CloseConnection follows with the leave. */
    val leavingByDoor = HashSet<Int>()

    val admittedSpectators = HashSet<Int>()

    var initialized = false

    var categoryAsked = false

    fun levelOf(playerId: Int): Int = present[playerId]?.level ?: 0

    fun visitors(): Int = present.values.count { !it.spectator }

    fun spectators(): Int = present.values.count { it.spectator }

    fun firstSpectator(): Int? =
        present.values
            .firstOrNull { it.spectator }
            ?.card
            ?.id

    fun controllers(): List<Int> = present.filterValues { it.level >= DoorCheck.RIGHTS }.keys.toList()

    fun ringingNamed(name: String): Pending? =
        ringing.values.firstOrNull { it.card.name.equals(name, ignoreCase = true) }
}
