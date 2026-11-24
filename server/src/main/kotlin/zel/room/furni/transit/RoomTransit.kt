package zel.room.furni.transit

/** Owned by its lock. */
class RoomTransit {
    val rides = LinkedHashMap<Int, Ride>()

    val pending = HashMap<Int, PendingUse>()

    val resets = mutableListOf<StateReset>()

    val gateCloses = HashMap<Int, GateClose>()

    val gateOpened = HashMap<Int, Long>()

    val passages = HashMap<Int, Passage>()

    val rolls = HashMap<Int, DiceRoll>()
}
