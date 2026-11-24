package zel.room.unit

class RoomUnits {
    private val byIndex = LinkedHashMap<Int, RoomUnit>()
    private val byPlayer = HashMap<Int, RoomUnit>()
    private var nextIndex = 0
    private val leaving = HashSet<Int>()

    fun nextIndex(): Int = nextIndex++

    fun add(unit: RoomUnit) {
        check(byIndex.putIfAbsent(unit.index, unit) == null) { "room index ${unit.index} is taken" }
        (unit.kind as? UnitKind.Player)?.let { byPlayer.putIfAbsent(it.playerId, unit) }
    }

    fun remove(index: Int): RoomUnit? =
        byIndex.remove(index)?.also { unit ->
            val playerId = (unit.kind as? UnitKind.Player)?.playerId
            if (playerId != null && byPlayer.remove(playerId, unit)) {
                byIndex.values.firstOrNull { (it.kind as? UnitKind.Player)?.playerId == playerId }?.let {
                    byPlayer[playerId] = it
                }
            }
        }

    operator fun get(index: Int): RoomUnit? = byIndex[index]

    fun all(): Collection<RoomUnit> = byIndex.values

    fun ofPlayer(playerId: Int): RoomUnit? = byPlayer[playerId]

    fun playerIds(): List<Int> = byIndex.values.mapNotNull { (it.kind as? UnitKind.Player)?.playerId }

    /** Runs [block] while [playerId] is on its way out: its unit is still here, but nothing is sent to it. */
    fun <T> whileLeaving(
        playerId: Int,
        block: () -> T,
    ): T {
        val first = leaving.add(playerId)
        try {
            return block()
        } finally {
            if (first) leaving.remove(playerId)
        }
    }

    fun isLeaving(playerId: Int): Boolean = playerId in leaving
}
